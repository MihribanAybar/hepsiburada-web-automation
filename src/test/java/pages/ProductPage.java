
package pages;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ProductPage: Hepsiburada ürün detay sayfası.
 *
 * Ürün adını ve seçili ana ürünün satış fiyatını okur.
 * Sepete Ekle butonunu kontrol eder.
 * Sepete ekleme onayını doğrular.
 *
 * Thread.sleep kullanılmaz.
 */
public class ProductPage {

    private final WebDriver driver;
    private final WebDriverWait wait;

    // Ürün başlığı
    private final By productTitle = By.cssSelector(
            "h1, [data-test-id='title']"
    );

    // Sepete Ekle butonu
    private final By addToCartButton = By.cssSelector(
            "#addToCart, " +
                    "button[data-test-id='addToCart'], " +
                    "[id*='addToCart']"
    );

    /*
     * Ana ürünün satış fiyatı için doğrulanan locator'lar.
     *
     * 1. MSI ve benzeri ürünler:
     * <div data-test-id="default-price">
     *     <div>
     *         <span>44.042,24 TL</span>
     *     </div>
     * </div>
     *
     * 2. Casper Excalibur G870:
     * <div class="bWwoI8vknB6COlRVbpRj">
     *     57.949,05 TL
     * </div>
     *
     * Diğer satıcı fiyatını gösteren
     * data-test-id="price-current-price"
     * kullanılmaz.
     */
    private final By[] productPriceLocators = {

            By.cssSelector(
                    "[data-test-id='default-price'] " +
                            "> div:first-child > span"
            ),

            By.cssSelector(
                    "div.bWwoI8vknB6COlRVbpRj"
            )
    };

    // Sepete ekleme sonrası gösterilen bildirimler
    private final By addToCartConfirmation = By.cssSelector(
            "[data-test-class='modal_overflow'], " +
                    "div[class*='checkoutui-Modal'], " +
                    "div[class*='notification'], " +
                    "div[class*='toast'], " +
                    "[class*='hb-toast'], " +
                    "div[role='dialog'], " +
                    "div[class*='modal']"
    );

    private static final Pattern PRICE_PATTERN =
            Pattern.compile(
                    "^\\s*(" +
                            "(?:\\d{1,3}(?:\\.\\d{3})+|\\d+)" +
                            "(?:,\\d{2})?" +
                            ")\\s*(?:TL|₺)\\s*$",
                    Pattern.CASE_INSENSITIVE
            );

    public ProductPage(WebDriver driver) {

        this.driver = driver;

        this.wait = new WebDriverWait(
                driver,
                Duration.ofSeconds(15)
        );
    }

    /**
     * Görünür ürün başlığını bulur.
     */
    private WebElement findVisibleProductTitle() {

        return wait.until(webDriver -> {

            List<WebElement> elements =
                    webDriver.findElements(productTitle);

            for (WebElement element : elements) {

                try {

                    if (!element.isDisplayed()) {
                        continue;
                    }

                    String text = element.getText().trim();

                    if (!text.isBlank()) {
                        return element;
                    }

                } catch (StaleElementReferenceException ignored) {
                    // DOM güncellenmiş olabilir.
                }
            }

            return null;
        });
    }

    /**
     * Başlıktaki ayrı marka etiketi varsa
     * ana ürün başlığını ayıklar.
     *
     * Güvenilir ayrım bulunamazsa
     * başlığın tamamını korur.
     */
    private String extractMainTitle(WebElement heading) {

        String fullText = heading.getText()
                .trim()
                .replaceAll("\\s+", " ");

        if (fullText.isBlank()) {

            throw new IllegalStateException(
                    "Ürün başlığı boş."
            );
        }

        JavascriptExecutor js =
                (JavascriptExecutor) driver;

        String longestTextNode = (String) js.executeScript(
                "const heading = arguments[0];" +
                        "const walker = document.createTreeWalker(" +
                        "heading, NodeFilter.SHOW_TEXT);" +
                        "let node;" +
                        "let longest = '';" +
                        "while ((node = walker.nextNode())) {" +
                        "  const value = node.textContent" +
                        "    .trim().replace(/\\s+/g, ' ');" +
                        "  if (value.length > longest.length) {" +
                        "    longest = value;" +
                        "  }" +
                        "}" +
                        "return longest;",
                heading
        );

        if (longestTextNode != null) {

            String candidate = longestTextNode
                    .trim()
                    .replaceAll("\\s+", " ");

            boolean meaningful =
                    candidate.length() >= 30;

            boolean coversMostOfTitle =
                    candidate.length() >=
                            fullText.length() * 0.75;

            boolean matchesHeadingEnd =
                    fullText.endsWith(candidate);

            if (meaningful
                    && coversMostOfTitle
                    && matchesHeadingEnd) {

                return candidate;
            }
        }

        return fullText;
    }

    /**
     * Ürün detay sayfasının açık olduğunu doğrular.
     */
    public boolean isOnProductPage() {

        try {

            String currentUrl =
                    driver.getCurrentUrl();

            boolean productUrl =
                    currentUrl.contains("-p-")
                            || currentUrl.contains("/p/");

            if (!productUrl) {
                return false;
            }

            WebElement heading =
                    findVisibleProductTitle();

            return !heading.getText().trim().isBlank();

        } catch (TimeoutException e) {

            return false;
        }
    }

    /**
     * Ürün başlığını kaydetmek için döndürür.
     */
    public String getProductTitle() {

        WebElement heading =
                findVisibleProductTitle();

        String rawTitle = heading.getText()
                .trim()
                .replaceAll("\\s+", " ");

        String title = extractMainTitle(heading);

        System.out.println(
                "Detay sayfası ham başlık: " + rawTitle
        );

        System.out.println(
                "Kaydedilecek ürün başlığı: " + title
        );

        return title;
    }

    /**
     * Metnin sadece tek bir TL fiyatından
     * oluştuğunu doğrular.
     */
    private String extractPrice(String text) {

        if (text == null || text.isBlank()) {
            return "";
        }

        String normalized = text.trim()
                .replace('\u00A0', ' ')
                .replaceAll("\\s+", " ");

        Matcher matcher =
                PRICE_PATTERN.matcher(normalized);

        if (matcher.matches()) {

            return matcher.group(1) + " TL";
        }

        return "";
    }

    /**
     * Ana ürünün güncel satış fiyatını okur.
     *
     * Önce default-price yapısını,
     * ardından Casper'da görülen div yapısını dener.
     *
     * Başka satıcının fiyatına geçiş yapmaz.
     */
    public String getProductPrice() {

        WebDriverWait priceWait = new WebDriverWait(
                driver,
                Duration.ofSeconds(8)
        );

        try {

            String price = priceWait.until(webDriver -> {

                for (By locator : productPriceLocators) {

                    List<WebElement> elements =
                            webDriver.findElements(locator);

                    for (WebElement element : elements) {

                        try {

                            if (!element.isDisplayed()) {
                                continue;
                            }

                            String text =
                                    element.getText().trim();

                            String amount =
                                    extractPrice(text);

                            if (!amount.isBlank()) {
                                return amount;
                            }

                        } catch (StaleElementReferenceException ignored) {
                            // Fiyat alanı yenilenmiş olabilir.
                        }
                    }
                }

                return null;
            });

            System.out.println(
                    "Ana ürün satış fiyatı: " + price
            );

            return price;

        } catch (TimeoutException e) {

            System.out.println(
                    "Ana ürünün satış fiyatı okunamadı."
            );

            return "";
        }
    }

    /**
     * Sepete Ekle butonunun aktifliğini kontrol eder.
     */
    public boolean isAddToCartButtonEnabled() {

        try {

            WebElement button = wait.until(
                    ExpectedConditions.elementToBeClickable(
                            addToCartButton
                    )
            );

            return button.isDisplayed()
                    && button.isEnabled();

        } catch (TimeoutException e) {

            return false;
        }
    }

    /**
     * Ürünü sepete ekler.
     *
     * Normal Selenium click kullanılır.
     */
    public void addToCart() {

        WebElement addButton = wait.until(
                ExpectedConditions.elementToBeClickable(
                        addToCartButton
                )
        );

        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView(" +
                        "{block:'center', inline:'nearest'});",
                addButton
        );

        addButton = wait.until(
                ExpectedConditions.elementToBeClickable(
                        addToCartButton
                )
        );

        addButton.click();

        System.out.println(
                "Sepete Ekle butonuna tıklandı."
        );
    }

    /**
     * Sepete ekleme onayını doğrular.
     */
    public boolean isAddedToCartConfirmationDisplayed() {

        try {

            WebDriverWait confirmationWait =
                    new WebDriverWait(
                            driver,
                            Duration.ofSeconds(8)
                    );

            return confirmationWait.until(webDriver -> {

                List<WebElement> notifications =
                        webDriver.findElements(
                                addToCartConfirmation
                        );

                for (WebElement notification : notifications) {

                    try {

                        if (!notification.isDisplayed()) {
                            continue;
                        }

                        String message =
                                notification.getText()
                                        .toLowerCase(
                                                Locale.forLanguageTag(
                                                        "tr-TR"
                                                )
                                        );

                        if (containsSuccessMessage(message)) {
                            return true;
                        }

                    } catch (StaleElementReferenceException ignored) {
                        // Modal yeniden çizilmiş olabilir.
                    }
                }

                return false;
            });

        } catch (TimeoutException e) {

            System.out.println(
                    "Sepete ekleme onay mesajı görünmedi."
            );

            return false;
        }
    }

    private boolean containsSuccessMessage(String message) {

        return message.contains("sepete eklendi")
                || message.contains("sepetinize eklendi")
                || message.contains("sepetine eklendi")
                || message.contains("ürün sepetinizde");
    }
}
