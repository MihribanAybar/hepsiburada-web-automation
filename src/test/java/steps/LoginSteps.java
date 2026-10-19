package steps;

import com.thoughtworks.gauge.Step;
import pages.HomePage;
import pages.LoginPage;
import support.DriverFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LoginSteps: Hepsiburada giriş işlemlerini yöneten
 * Gauge Step Implementation sınıfıdır.
 *
 * Kullanıcı bilgilerini ortam değişkenlerinden alır
 * ve başarılı giriş işlemini doğrular.
 */
public class LoginSteps {

    private LoginPage loginPage;
    private HomePage homePage;

    /**
     * LoginPage nesnesini ihtiyaç olduğunda oluşturur.
     */
    private LoginPage getLoginPage() {

        if (loginPage == null) {
            loginPage = new LoginPage(
                    DriverFactory.getDriver()
            );
        }

        return loginPage;
    }

    /**
     * HomePage nesnesini ihtiyaç olduğunda oluşturur.
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
     * Geçerli kullanıcı bilgileriyle giriş yapar.
     */
    @Step("Geçerli kullanıcı bilgileriyle giriş yap")
    public void loginWithValidCredentials() {

        String email = System.getenv("HB_EMAIL");
        String password = System.getenv("HB_PASSWORD");

        assertThat(email)
                .as("HB_EMAIL ortam değişkeni tanımlanmalıdır.")
                .isNotBlank();

        assertThat(password)
                .as("HB_PASSWORD ortam değişkeni tanımlanmalıdır.")
                .isNotBlank();

        getLoginPage().enterEmail(email);
        getLoginPage().enterPassword(password);
        getLoginPage().submitLogin();

        System.out.println(
                "Giriş işlemi gerçekleştirildi."
        );
    }

    /**
     * Giriş işleminin başarılı olduğunu doğrular.
     */
    @Step("Kullanıcının başarılı şekilde giriş yaptığını doğrula")
    public void verifyLogin() {

        assertThat(
                getHomePage().isLoggedIn()
        )
                .as("Kullanıcı başarılı şekilde giriş yapmış olmalıdır.")
                .isTrue();

        System.out.println(
                "Kullanıcı giriş kontrolü tamamlandı."
        );
    }
}
