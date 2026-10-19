package steps;

import com.thoughtworks.gauge.Step;
import pages.HomePage;
import support.DriverFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HomeSteps: Hepsiburada ana sayfasına ait
 * Gauge adım uygulamalarını içerir.
 *
 * Ana sayfaya yönlenme ve giriş sayfasını
 * açma işlemlerini yönetir.
 */
public class HomeSteps {

    private HomePage homePage;

    /**
     * HomePage nesnesini ihtiyaç duyulduğunda oluşturur.
     */
    private HomePage getHomePage() {

        if (homePage == null) {
            homePage = new HomePage(
                    DriverFactory.getDriver()
            );
        }

        return homePage;
    }

    /**
     * Tarayıcının başlatıldığını kontrol eder
     * ve Hepsiburada ana sayfasını açar.
     */
    @Step("Tarayıcıyı aç ve <url> adresine git")
    public void openBrowserAndNavigate(String url) {

        assertThat(url)
                .as("Site adresi boş olmamalıdır.")
                .isNotBlank();

        String expectedUrl = "https://www.hepsiburada.com";

        String normalizedUrl = url.trim()
                .replaceAll("/+$", "");

        assertThat(normalizedUrl)
                .as("Senaryoda doğru Hepsiburada adresi kullanılmalıdır.")
                .isEqualTo(expectedUrl);

        assertThat(DriverFactory.getDriver())
                .as("WebDriver başlatılmış olmalıdır.")
                .isNotNull();

        getHomePage().open();

        System.out.println(
                "Hepsiburada ana sayfası açıldı."
        );
    }

    /**
     * Ana sayfadaki Giriş Yap menüsünü açar
     * ve giriş sayfasına yönlenir.
     */
    @Step("Sağ üstteki Giriş Yap butonuna tıkla")
    public void clickLoginButton() {

        getHomePage().openLogin();

        System.out.println(
                "Giriş sayfasına yönlendirildi."
        );
    }
}
