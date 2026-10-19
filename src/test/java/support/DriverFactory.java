
package support;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.FluentWait;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * DriverFactory: WebDriver yaşam döngüsünü yönetir.
 *
 * Chrome bağımsız işletim sistemi süreci olarak açılır.
 * Selenium remote debugging üzerinden Chrome'a bağlanır.
 *
 * Her senaryoda yeni, temiz bir Chrome profili kullanılır.
 * Parola kaydetme özelliği Chrome açılmadan önce kapatılır.
 */
public final class DriverFactory {

    private static final int DEBUG_PORT = 9222;

    private static WebDriver driver;
    private static Process chromeProcess;
    private static Path profileDirectory;

    private DriverFactory() {
    }

    /**
     * Windows'ta Chrome uygulamasını bulur.
     */
    private static String findChromePath() {

        String localAppData = System.getenv("LOCALAPPDATA");
        String programFiles = System.getenv("PROGRAMFILES");

        String[] paths = {
                "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe",
                "C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe",
                localAppData == null
                        ? ""
                        : localAppData
                        + "\\Google\\Chrome\\Application\\chrome.exe",
                programFiles == null
                        ? ""
                        : programFiles
                        + "\\Google\\Chrome\\Application\\chrome.exe"
        };

        for (String path : paths) {

            if (!path.isEmpty() && new File(path).isFile()) {
                return path;
            }
        }

        throw new IllegalStateException(
                "Google Chrome bulunamadı. " +
                        "Chrome kurulum yolunu kontrol edin."
        );
    }

    /**
     * Remote debugging portunun kullanımda olup
     * olmadığını kontrol eder.
     */
    private static boolean isPortInUse() {

        try (Socket socket = new Socket()) {

            socket.connect(
                    new InetSocketAddress(
                            "127.0.0.1",
                            DEBUG_PORT
                    ),
                    500
            );

            return true;

        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Chrome'un debugging bağlantısının
     * hazır olup olmadığını kontrol eder.
     */
    private static boolean isChromeReady() {

        HttpURLConnection connection = null;

        try {

            URI uri = URI.create(
                    "http://127.0.0.1:" +
                            DEBUG_PORT +
                            "/json/version"
            );

            connection = (HttpURLConnection)
                    uri.toURL().openConnection();

            connection.setConnectTimeout(500);
            connection.setReadTimeout(500);

            return connection.getResponseCode() == 200;

        } catch (IOException e) {

            return false;

        } finally {

            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Chrome'un hazırlanmasını FluentWait ile bekler.
     * Thread.sleep kullanılmaz.
     */
    private static void waitForChromeReady() {

        new FluentWait<>(chromeProcess)
                .withTimeout(Duration.ofSeconds(20))
                .pollingEvery(Duration.ofMillis(250))
                .until(process -> {

                    if (!process.isAlive()) {

                        throw new IllegalStateException(
                                "Chrome süreci beklenmedik " +
                                        "şekilde kapandı."
                        );
                    }

                    return isChromeReady();
                });
    }

    /**
     * Yeni Chrome profilini başlangıçtan önce hazırlar.
     *
     * Önemli:
     * Chrome'u ProcessBuilder ile açtığımız için
     * şifre yöneticisi ayarlarını ChromeOptions ile
     * sonradan göndermiyoruz.
     */
    private static void prepareChromeProfile()
            throws IOException {

        Path defaultProfile = profileDirectory.resolve(
                "Default"
        );

        Files.createDirectories(defaultProfile);

        Path preferencesFile = defaultProfile.resolve(
                "Preferences"
        );

        String preferences = """
                {
                  "credentials_enable_service": false,
                  "profile": {
                    "password_manager_enabled": false,
                    "password_manager_leak_detection": false
                  }
                }
                """;

        Files.writeString(
                preferencesFile,
                preferences,
                StandardCharsets.UTF_8
        );

        System.out.println(
                "Yeni Chrome profili hazırlandı. " +
                        "Parola kaydetme tercihi kapatıldı."
        );
    }

    /**
     * Chrome'u bağımsız süreç olarak başlatır.
     *
     * Her çalıştırmada yeni bir geçici profil oluşturur.
     */
    public static synchronized void startBrowser() {

        if (driver != null) {
            return;
        }

        try {

            if (isPortInUse()) {

                throw new IllegalStateException(
                        "9222 portu kullanımda. " +
                                "Önceki test Chrome oturumlarını kontrol edin."
                );
            }

            String chromeExe = findChromePath();

            // Her senaryo için yeni profil.
            profileDirectory = Files.createTempDirectory(
                    "hb-chrome-profile-"
            );

            // Chrome başlamadan önce profil ayarları.
            prepareChromeProfile();

            // Chrome bağımsız Windows süreci olarak açılır.
            ProcessBuilder pb = new ProcessBuilder(
                    chromeExe,
                    "--remote-debugging-address=127.0.0.1",
                    "--remote-debugging-port=" + DEBUG_PORT,
                    "--user-data-dir="
                            + profileDirectory.toAbsolutePath(),
                    "--start-maximized",
                    "--no-first-run",
                    "--no-default-browser-check",
                    "--disable-notifications",
                    "--disable-session-crashed-bubble"
            );

            pb.redirectErrorStream(true);
            pb.redirectOutput(
                    ProcessBuilder.Redirect.DISCARD
            );

            chromeProcess = pb.start();

            // Chrome debugging servisini bekle.
            waitForChromeReady();

            // Selenium yalnızca çalışan Chrome'a bağlanır.
            ChromeOptions debugOptions = new ChromeOptions();

            debugOptions.setExperimentalOption(
                    "debuggerAddress",
                    "127.0.0.1:" + DEBUG_PORT
            );

            driver = new ChromeDriver(debugOptions);

            // Bekleme işlemlerinde explicit wait kullanılacak.
            driver.manage().timeouts()
                    .implicitlyWait(Duration.ZERO)
                    .pageLoadTimeout(Duration.ofSeconds(30))
                    .scriptTimeout(Duration.ofSeconds(15));

            System.out.println(
                    "Chrome bağımsız süreç olarak başlatıldı."
            );

            System.out.println(
                    "Selenium Chrome'a başarıyla bağlandı."
            );

        } catch (Exception e) {

            quitBrowser();

            throw new IllegalStateException(
                    "Chrome başlatılamadı: " + e.getMessage(),
                    e
            );
        }
    }

    /**
     * Aktif WebDriver nesnesini döndürür.
     * Henüz yoksa Chrome'u başlatır.
     */
    public static synchronized WebDriver getDriver() {

        if (driver == null) {
            startBrowser();
        }

        return driver;
    }

    /**
     * Yalnızca test için oluşturulan geçici
     * Chrome profilini temizler.
     */
    private static void deleteProfileDirectory() {

        if (profileDirectory == null) {
            return;
        }

        try (Stream<Path> paths =
                     Files.walk(profileDirectory)) {

            for (Path path : paths
                    .sorted(Comparator.reverseOrder())
                    .toList()) {

                Files.deleteIfExists(path);
            }

            System.out.println(
                    "Geçici Chrome profili temizlendi."
            );

        } catch (IOException e) {

            System.out.println(
                    "UYARI: Geçici profil tamamen silinemedi: "
                            + e.getMessage()
            );

        } finally {

            profileDirectory = null;
        }
    }

    /**
     * Senaryo bitince WebDriver ve Chrome'u kapatır.
     * Ardından test profilini temizler.
     */
    public static synchronized void quitBrowser() {

        if (driver != null) {

            try {

                driver.quit();

            } catch (Exception e) {

                System.out.println(
                        "WebDriver kapatılırken hata: "
                                + e.getMessage()
                );

            } finally {

                driver = null;
            }
        }

        if (chromeProcess != null) {

            try {

                if (chromeProcess.isAlive()) {

                    chromeProcess.destroy();

                    boolean closed = chromeProcess.waitFor(
                            5,
                            TimeUnit.SECONDS
                    );

                    if (!closed) {

                        chromeProcess.destroyForcibly();

                        chromeProcess.waitFor(
                                5,
                                TimeUnit.SECONDS
                        );
                    }
                }

            } catch (InterruptedException e) {

                Thread.currentThread().interrupt();

            } finally {

                chromeProcess = null;
            }
        }

        deleteProfileDirectory();

        System.out.println(
                "Tarayıcı oturumu kapatıldı."
        );
    }
}
