package pages;

import org.openqa.selenium.By;
import support.LocatorReader;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * LoginPage: Hepsiburada giriş sayfasını temsil eden
 * Page Object Model sınıfıdır.
 *
 * E-posta/telefon girişi, şifre girişi ve
 * giriş butonuna tıklama işlemlerini yönetir.
 */
public class LoginPage {

    private final WebDriver driver;
    private final WebDriverWait wait;

    // E-posta veya telefon giriş alanı
    private final By emailInput =
            LocatorReader.getLocator("Login/login.json", "emailInput");

    // Şifre giriş alanı
    private final By passwordInput =
            LocatorReader.getLocator("Login/login.json", "passwordInput");

    // Giriş yap butonu
    private final By submitButton =
            LocatorReader.getLocator("Login/login.json", "submitButton");

    public LoginPage(WebDriver driver) {

        this.driver = driver;

        this.wait = new WebDriverWait(
                driver,
                Duration.ofSeconds(15)
        );
    }

    /**
     * Belirtilen input alanına metin yazar.
     * Alanın görünür ve aktif olmasını bekler.
     */
    private void enterText(By locator, String text) {

        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException(
                    "Giriş alanına boş değer gönderilemez."
            );
        }

        WebElement input = wait.until(
                ExpectedConditions.elementToBeClickable(
                        locator
                )
        );

        input.click();
        input.clear();
        input.sendKeys(text);

        // Girilen değerin input'a yazıldığını doğrula.
        wait.until(
                ExpectedConditions.attributeToBe(
                        locator,
                        "value",
                        text
                )
        );
    }

    /**
     * Giriş sayfasında e-posta/telefon alanını doldurur.
     */
    public void enterEmail(String email) {

        // Gerçekten giriş sayfasında mıyız?
        if (!driver.getCurrentUrl().contains(
                "giris.hepsiburada.com")) {

            throw new IllegalStateException(
                    "Giriş sayfasında değiliz. " +
                            "Önce ana sayfadan Giriş Yap butonuna tıklanmalı."
            );
        }

        enterText(emailInput, email);
    }

    /**
     * Şifre alanını doldurur.
     */
    public void enterPassword(String password) {

        enterText(passwordInput, password);
    }

    /**
     * Giriş Yap butonuna tıklar.
     * Giriş sayfasından yönlendirmeyi bekler.
     */
    public void submitLogin() {

        WebElement button = wait.until(
                ExpectedConditions.elementToBeClickable(
                        submitButton
                )
        );

        try {
            button.click();

        } catch (ElementClickInterceptedException e) {

            // Tıklama başka bir element tarafından engellenirse
            // Actions üzerinden tekrar dene.
            button = wait.until(
                    ExpectedConditions.elementToBeClickable(
                            submitButton
                    )
            );

            new Actions(driver)
                    .moveToElement(button)
                    .click()
                    .perform();
        }

        // Giriş sayfasından çıkılmasını bekle.
        wait.until(
                ExpectedConditions.not(
                        ExpectedConditions.urlContains(
                                "giris.hepsiburada.com"
                        )
                )
        );
    }
}
