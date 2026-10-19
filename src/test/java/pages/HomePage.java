
package pages;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

/**
 * HomePage: Hepsiburada ana sayfasını temsil eder.
 *
 * Ana sayfayı açma, çerez kabulü,
 * login sayfasına yönlendirme ve arama
 * işlemlerini yönetir.
 */
public class HomePage {

    private static final String HOME_URL =
            "https://www.hepsiburada.com";

    private final WebDriver driver;
    private final WebDriverWait wait;

    // Çerez kabul butonu
    private final By cookieAcceptBtn =
            By.id("onetrust-accept-btn-handler");

    // Sağ üstteki hesap menüsü
    private final By myAccountButton =
            By.id("myAccount");

    // Hesap menüsündeki giriş bağlantısı
    private final By loginItem = By.cssSelector(
            "#login, a[href*='giris.hepsiburada.com']"
    );

    /*
     * AŞAMA 1:
     *
     * Henüz açılmamış arama alanının dış kapsayıcısı.
     *
     * Kullanıcının gönderdiği HTML:
     *
     * role="search"
     * aria-label="Site İçi Arama"
     *
     * Bu alana tıklanınca geniş arama paneli açılır.
     */
    private final By searchOpeningArea = By.cssSelector(
            "[role='search'][aria-label='Site İçi Arama']"
    );

    /*
     * AŞAMA 2:
     *
     * Açılan geniş arama panelindeki gerçek input.
     *
     * Kullanıcının gönderdiği HTML:
     *
     * class="searchBarContent-..."
     * data-test-id="search-bar-input"
     *
     * İlk aşamadaki initialComponent input'u
     * bu locator ile seçilmez.
     */
    private final By activeSearchInput = By.cssSelector(
            "input[data-test-id='search-bar-input']" +
                    "[class*='searchBarContent-']"
    );

    public HomePage(WebDriver driver) {

        this.driver = driver;

        this.wait = new WebDriverWait(
                driver,
                Duration.ofSeconds(20)
        );
    }

    /**
     * Çerez penceresi görünüyorsa kabul eder.
     */
    public void acceptCookiesIfExists() {

        List<WebElement> buttons =
                driver.findElements(cookieAcceptBtn);

        for (WebElement button : buttons) {

            try {

                if (button.isDisplayed()
                        && button.isEnabled()) {

                    button.click();

                    wait.until(
                            ExpectedConditions
                                    .invisibilityOfElementLocated(
                                            cookieAcceptBtn
                                    )
                    );

                    return;
                }

            } catch (StaleElementReferenceException e) {

                return;

            } catch (ElementClickInterceptedException e) {

                throw new IllegalStateException(
                        "Çerez kabul butonuna başka " +
                                "bir element engel oluyor.",
                        e
                );
            }
        }
    }

    /**
     * Ana sayfayı açar.
     */
    public void open() {

        driver.get(HOME_URL);

        wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        myAccountButton
                )
        );

        acceptCookiesIfExists();

        System.out.println(
                "Hepsiburada ana sayfası açıldı."
        );
    }

    /**
     * Hesap menüsünden giriş sayfasını açar.
     */
    public void openLogin() {

        acceptCookiesIfExists();

        WebElement accountButton = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        myAccountButton
                )
        );

        new Actions(driver)
                .moveToElement(accountButton)
                .perform();

        WebElement loginButton = wait.until(
                ExpectedConditions.elementToBeClickable(
                        loginItem
                )
        );

        loginButton.click();

        wait.until(
                ExpectedConditions.urlContains(
                        "giris.hepsiburada.com"
                )
        );

        System.out.println(
                "Giriş sayfasına yönlendirildi."
        );
    }

    /**
     * Açılan arama panelindeki aktif input'u bulur.
     *
     * initialComponent input'u burada kullanılmaz.
     */
    private WebElement getActiveSearchInput() {

        return wait.until(webDriver -> {

            List<WebElement> elements =
                    webDriver.findElements(activeSearchInput);

            for (WebElement element : elements) {

                try {

                    if (element.isDisplayed()
                            && element.isEnabled()) {

                        return element;
                    }

                } catch (StaleElementReferenceException ignored) {

                    // Arama paneli yeniden oluşturulmuş olabilir.
                }
            }

            return null;
        });
    }

    /**
     * Ürün araması yapar.
     *
     * 1. Kapalı arama alanına tıklar.
     * 2. Büyük arama panelinin açılmasını bekler.
     * 3. Paneldeki gerçek input'a yazı yazar.
     * 4. Enter ile sonuç sayfasına yönlenir.
     */
    public void search(String keyword) {

        if (keyword == null || keyword.isBlank()) {

            throw new IllegalArgumentException(
                    "Arama kelimesi boş olamaz."
            );
        }

        String currentUrl = driver.getCurrentUrl();

        // Ana site dışında kalınmışsa ana sayfayı aç.
        if (!currentUrl.startsWith(HOME_URL)) {

            open();
        }

        acceptCookiesIfExists();

        /*
         * BİRİNCİ AŞAMA
         *
         * Sayfanın başlangıçtaki arama bileşenine tıkla.
         * Pasif input'a clear veya sendKeys uygulama.
         */

        WebElement openingArea = wait.until(
                ExpectedConditions.elementToBeClickable(
                        searchOpeningArea
                )
        );

        openingArea.click();

        System.out.println(
                "Arama alanına tıklandı."
        );

        /*
         * İKİNCİ AŞAMA
         *
         * Arama paneli açılınca oluşan
         * searchBarContent input'unu bekle.
         */

        WebElement input = getActiveSearchInput();

        System.out.println(
                "Aktif arama kutusu açıldı."
        );

        // Açılan paneldeki gerçek input'a odaklan.
        input.click();

        // Önceki bir metin varsa seçip üzerine yaz.
        input.sendKeys(
                Keys.chord(Keys.CONTROL, "a")
        );

        input.sendKeys(keyword);

        /*
         * Gönderilen metnin gerçekten
         * aktif input'a yazıldığını doğrula.
         */
        try {

            wait.until(webDriver -> {

                try {

                    WebElement currentInput =
                            getVisibleActiveInputNow();

                    if (currentInput == null) {
                        return false;
                    }

                    return keyword.equals(
                            currentInput.getAttribute("value")
                    );

                } catch (StaleElementReferenceException e) {

                    return false;
                }
            });

        } catch (TimeoutException e) {

            throw new IllegalStateException(
                    "Aktif arama kutusuna '" +
                            keyword +
                            "' yazıldığı doğrulanamadı.",
                    e
            );
        }

        System.out.println(
                "Arama kutusuna yazıldı: " + keyword
        );

        // Enter'dan önceki adresi kaydet.
        String urlBeforeSearch = driver.getCurrentUrl();

        // Arama alanı değişmiş olabileceği için
        // güncel elementi tekrar bul.
        WebElement inputForEnter = getActiveSearchInput();

        inputForEnter.sendKeys(Keys.ENTER);

        // Arama sonuçlarına yönlendirmeyi bekle.
        wait.until(
                ExpectedConditions.not(
                        ExpectedConditions.urlToBe(
                                urlBeforeSearch
                        )
                )
        );

        System.out.println(
                "Arama başlatıldı: " + keyword
        );
    }

    /**
     * Aktif input'u bekleme yapmadan bulur.
     * WebDriverWait koşulları içinde kullanılır.
     */
    private WebElement getVisibleActiveInputNow() {

        for (WebElement element :
                driver.findElements(activeSearchInput)) {

            try {

                if (element.isDisplayed()
                        && element.isEnabled()) {

                    return element;
                }

            } catch (StaleElementReferenceException ignored) {

                // Bir sonraki denemede yeniden aranır.
            }
        }

        return null;
    }

    /**
     * Hesap menüsüne göre giriş durumunu
     * ön kontrol olarak değerlendirir.
     */
    public boolean isLoggedIn() {

        try {

            WebElement account = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            myAccountButton
                    )
            );

            String text = account.getText().trim();

            return !text.isBlank()
                    && !text.contains("Giriş Yap");

        } catch (TimeoutException e) {

            return false;
        }
    }
}
