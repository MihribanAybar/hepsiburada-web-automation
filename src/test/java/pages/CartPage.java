
package pages;

import org.openqa.selenium.By;
import support.LocatorReader;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CartPage: Hepsiburada sepet islemleri.
 *
 * - Header sepet sayisini kontrol eder.
 * - Eski urunleri temizler.
 * - Sepete ekleme modalinden sepete gider.
 * - Urun adi, fiyat ve adet bilgilerini dogrular.
 *
 * Thread.sleep kullanilmaz.
 */
public class CartPage {

    private static final String HOME_URL =
            "https://www.hepsiburada.com/";

    private static final String CHECKOUT_URL =
            "https://checkout.hepsiburada.com/";

    private final WebDriver driver;
    private final WebDriverWait wait;

    private final By headerCartCount =
            LocatorReader.getLocator("Shopping/cart.json", "headerCartCount");

    private final By cartLink = LocatorReader.getLocator("Shopping/cart.json", "cartLink");

    private final By cookieAcceptBtn =
            LocatorReader.getLocator("Shopping/cart.json", "cookieAcceptBtn");

    private final By addToCartModal = LocatorReader.getLocator("Shopping/cart.json", "addToCartModal");

    private final By modalGoToCartButton = LocatorReader.getLocator("Shopping/cart.json", "modalGoToCartButton");

    private final By deleteAllProductsButton = LocatorReader.getLocator("Shopping/cart.json", "deleteAllProductsButton");

    private final By confirmDeleteAllButton = LocatorReader.getLocator("Shopping/cart.json", "confirmDeleteAllButton");

    private final By[] cartRowLocators = LocatorReader.getLocators("Shopping/cart.json", "cartRowLocators");

    private final By productName = LocatorReader.getLocator("Shopping/cart.json", "productName");

    /*
     * GERCEK URUN SATIRI FIYATI
     *
     * DevTools'ta dogrulanan HTML:
     *
     * <div class="product_price_uXU6Q">
     *     44.042,24 TL
     * </div>
     *
     * Kazancini gor, eski fiyat ve
     * indirim kapsayicilari secilmez.
     */
    private final By productPrice = LocatorReader.getLocator("Shopping/cart.json", "productPrice");

    private final By quantitySelect = LocatorReader.getLocator("Shopping/cart.json", "quantitySelect");

    private final By quantityInput = LocatorReader.getLocator("Shopping/cart.json", "quantityInput");

    private final By quantityText = LocatorReader.getLocator("Shopping/cart.json", "quantityText");

    /*
     * Yalnizca tek bir fiyat tutarini kabul eder.
     *
     * Gecerli:
     * 35.999,00 TL
     * 999,90 TL
     * 44.042 TL
     *
     * Gecersiz:
     * Kazancini gor / 3.000 TL / 35.999 TL
     */
    private static final Pattern SINGLE_PRICE_PATTERN =
            Pattern.compile(
                    "^\\s*(" +
                            "(?:\\d{1,3}(?:\\.\\d{3})+|\\d+)" +
                            "(?:,\\d{2})?" +
                            ")\\s*(?:TL|₺)\\s*$",
                    Pattern.CASE_INSENSITIVE
            );

    public CartPage(WebDriver driver) {

        this.driver = driver;

        this.wait = new WebDriverWait(
                driver,
                Duration.ofSeconds(20)
        );
    }

    /**
     * Cerez bildirimi varsa kabul eder.
     */
    public void acceptCookiesIfExists() {

        for (WebElement element :
                driver.findElements(cookieAcceptBtn)) {

            try {

                if (element.isDisplayed()
                        && element.isEnabled()) {

                    element.click();
                    return;
                }

            } catch (StaleElementReferenceException ignored) {
                // DOM degismis olabilir.
            }
        }
    }

    /**
     * Sepete ekleme modalini kontrol eder.
     */
    private boolean isAddToCartModalVisible() {

        for (WebElement modal :
                driver.findElements(addToCartModal)) {

            try {

                if (modal.isDisplayed()) {
                    return true;
                }

            } catch (StaleElementReferenceException ignored) {
                // Sonraki denemede yeniden aranir.
            }
        }

        return false;
    }

    /**
     * Modal icindeki Sepete git butonunu bulur.
     */
    private WebElement findVisibleModalCartButton() {

        for (WebElement element :
                driver.findElements(modalGoToCartButton)) {

            try {

                if (!element.isDisplayed()
                        || !element.isEnabled()) {
                    continue;
                }

                List<WebElement> parents =
                        element.findElements(
                                LocatorReader.getLocator("Shopping/cart.json", "modalClickableAncestor")
                        );

                if (!parents.isEmpty()) {

                    WebElement parent = parents.get(0);

                    if (parent.isDisplayed()
                            && parent.isEnabled()) {
                        return parent;
                    }
                }

                return element;

            } catch (StaleElementReferenceException ignored) {
                // Modal yeniden cizilmis olabilir.
            }
        }

        return null;
    }

    /**
     * Gercek sepet sayfasinin yuklendigini kontrol eder.
     *
     * Sepet adresi /sepetim veya checkout kok adresi
     * olabilir. Bu nedenle sepet elementini de arar.
     *
     * Bu kontrol dolu sepet icin kullanilir.
     */
    private boolean isCartPageLoadedNow() {

        String currentUrl = driver.getCurrentUrl();

        if (!currentUrl.startsWith(CHECKOUT_URL)) {
            return false;
        }

        for (WebElement element :
                driver.findElements(deleteAllProductsButton)) {

            try {

                if (element.isDisplayed()) {
                    return true;
                }

            } catch (StaleElementReferenceException ignored) {
                // Sayfa yeniden yukleniyor olabilir.
            }
        }

        return false;
    }

    /**
     * Sepet sayfasini acar.
     *
     * Modal varsa Sepete git butonunu kullanir.
     * Modal yoksa header Sepetim baglantisina tiklar.
     */
    public void openCart() {

        if (isCartPageLoadedNow()) {

            System.out.println(
                    "Sepet sayfası zaten açık ve yüklü."
            );

            return;
        }

        WebElement modalButton =
                findVisibleModalCartButton();

        boolean modalVisible =
                isAddToCartModalVisible();

        if (modalButton != null || modalVisible) {

            System.out.println(
                    "Sepete ekleme penceresi algılandı."
            );

            WebElement goToCartButton = wait.until(
                    webDriver ->
                            findVisibleModalCartButton()
            );

            goToCartButton.click();

            System.out.println(
                    "Modal içindeki Sepete git butonuna tıklandı."
            );

        } else {

            WebElement headerButton = wait.until(
                    ExpectedConditions.elementToBeClickable(
                            cartLink
                    )
            );

            headerButton.click();

            System.out.println(
                    "Header'daki Sepetim bağlantısına tıklandı."
            );
        }

        try {

            wait.until(webDriver ->
                    isCartPageLoadedNow()
            );

        } catch (TimeoutException e) {

            throw new IllegalStateException(
                    "Sepete git işleminden sonra " +
                            "gerçek sepet ekranı yüklenmedi. URL: " +
                            driver.getCurrentUrl(),
                    e
            );
        }

        acceptCookiesIfExists();

        System.out.println(
                "Sepet sayfası doğrulandı: " +
                        driver.getCurrentUrl()
        );
    }

    /**
     * Header sepet sayisini bekleme olmadan okur.
     */
    private int readHeaderCartCountNow() {

        for (WebElement element :
                driver.findElements(headerCartCount)) {

            try {

                String value = element.getText().trim();

                if (value.isBlank()) {

                    value = element.getAttribute(
                            "textContent"
                    );

                    if (value == null) {
                        return -1;
                    }

                    value = value.trim();
                }

                if (value.matches("\\d+")) {
                    return Integer.parseInt(value);
                }

            } catch (StaleElementReferenceException ignored) {
                return -1;
            }
        }

        return -1;
    }

    /**
     * Header sepet sayisinin okunmasini bekler.
     */
    public int getHeaderCartCount() {

        return wait.until(webDriver -> {

            int count = readHeaderCartCountNow();

            return count >= 0 ? count : null;
        });
    }

    /**
     * Baslangicta sepeti kontrol eder.
     *
     * Bos ise sepete girmez.
     * Dolu ise tum eski urunleri temizler.
     */
    public void clearCart() {

        int initialCount = getHeaderCartCount();

        System.out.println(
                "Başlangıç sepet ürün sayısı: " +
                        initialCount
        );

        if (initialCount == 0) {

            System.out.println(
                    "Sepet zaten boş. Sepet sayfasına girilmiyor."
            );

            return;
        }

        System.out.println(
                "Sepette ürün var. Temizleme başlıyor."
        );

        openCart();

        WebElement deleteButton = wait.until(
                ExpectedConditions.elementToBeClickable(
                        deleteAllProductsButton
                )
        );

        deleteButton.click();

        System.out.println(
                "Ürünleri sil butonuna tıklandı."
        );

        WebElement confirmButton = wait.until(
                ExpectedConditions.elementToBeClickable(
                        confirmDeleteAllButton
                )
        );

        confirmButton.click();

        System.out.println(
                "Tümünü sil butonuna tıklandı."
        );

        wait.until(
                ExpectedConditions.invisibilityOfElementLocated(
                        confirmDeleteAllButton
                )
        );

        driver.navigate().to(HOME_URL);

        try {

            wait.until(webDriver ->
                    readHeaderCartCountNow() == 0
            );

        } catch (TimeoutException e) {

            throw new IllegalStateException(
                    "Sepet temizlendikten sonra " +
                            "header sayısı 0 olmadı. Son değer: " +
                            readHeaderCartCountNow(),
                    e
            );
        }

        System.out.println(
                "Sepet temizlendi. Header ürün sayısı: 0"
        );
    }

    /**
     * Sepetin boslugunu header uzerinden dogrular.
     */
    public boolean isCartEmpty() {

        try {

            return getHeaderCartCount() == 0;

        } catch (TimeoutException e) {

            return false;
        }
    }

    /**
     * Gorunur sepet urun satirlarini bulur.
     */
    private List<WebElement> getCartRows() {

        for (By locator : cartRowLocators) {

            List<WebElement> rows = new ArrayList<>();

            for (WebElement element :
                    driver.findElements(locator)) {

                try {

                    if (element.isDisplayed()) {
                        rows.add(element);
                    }

                } catch (StaleElementReferenceException ignored) {
                    // DOM degismis olabilir.
                }
            }

            if (!rows.isEmpty()) {
                return rows;
            }
        }

        return new ArrayList<>();
    }

    /**
     * Urun satirlarinin yuklenmesini bekler.
     */
    private void waitForCartProducts() {

        wait.until(webDriver ->
                !getCartRows().isEmpty()
        );
    }

    public int getCartProductCount() {

        waitForCartProducts();

        return getCartRows().size();
    }

    /**
     * Ilk urun satirini dondurur.
     */
    private WebElement getFirstCartRow() {

        waitForCartProducts();

        List<WebElement> rows = getCartRows();

        if (rows.isEmpty()) {

            throw new IllegalStateException(
                    "Sepette ürün satırı bulunamadı."
            );
        }

        return rows.get(0);
    }

    /**
     * Sepetteki urun adini okur.
     */
    public String getCartProductName() {

        WebElement row = getFirstCartRow();

        for (WebElement element :
                row.findElements(productName)) {

            if (element.isDisplayed()) {

                String name = element.getText().trim();

                if (!name.isBlank()) {
                    return name;
                }
            }
        }

        throw new IllegalStateException(
                "Sepetteki ürün adı bulunamadı."
        );
    }

    /**
     * Sepetteki urun satirinin TEK satis fiyatini okur.
     *
     * Indirim metni, eski fiyat ve
     * "Kazancini gor" kapsayicisi kabul edilmez.
     */
    public String getCartProductPrice() {

        try {

            String price = wait.withMessage(
                    "Sepet ürün satırında tek satış " +
                            "fiyatı içeren product_price_ " +
                            "elementi bulunamadı."
            ).until(webDriver -> {

                try {

                    WebElement row = getFirstCartRow();

                    List<WebElement> priceElements =
                            row.findElements(productPrice);

                    for (WebElement element : priceElements) {

                        if (!element.isDisplayed()) {
                            continue;
                        }

                        String text = element.getText()
                                .trim()
                                .replace('\u00A0', ' ');

                        // Yalnizca tek tutardan olusan
                        // metin kabul edilir.
                        if (SINGLE_PRICE_PATTERN
                                .matcher(text)
                                .matches()) {

                            return text;
                        }
                    }

                    return null;

                } catch (StaleElementReferenceException e) {

                    return null;
                }
            });

            System.out.println(
                    "Sepetteki gerçek ürün fiyatı: " + price
            );

            return price;

        } catch (TimeoutException e) {

            throw new IllegalStateException(
                    "Sepetteki ürünün tek satış fiyatı " +
                            "okunamadı. product_price_ locator'ını " +
                            "ürün satırının HTML'iyle kontrol edin.",
                    e
            );
        }
    }

    /**
     * Metinleri karsilastirma icin normalize eder.
     */
    private String normalizeText(String text) {

        return text.toLowerCase(
                        Locale.forLanguageTag("tr-TR")
                )
                .replaceAll("[^\\p{L}\\p{N}]+", " ")
                .trim()
                .replaceAll("\\s+", " ");
    }

    /**
     * Kaydedilen urun adiyla sepet adini karsilastirir.
     *
     * Mevcut kismi eslesme davranisi korunmustur.
     */
    public boolean isProductNameMatching(
            String expectedTitle) {

        if (expectedTitle == null
                || expectedTitle.isBlank()) {

            return false;
        }

        String actualTitle = getCartProductName();

        String expected = normalizeText(expectedTitle);
        String actual = normalizeText(actualTitle);

        return !actual.isBlank()
                && (actual.equals(expected)
                || actual.contains(expected));
    }

    public boolean containsProduct(String expectedName) {

        try {

            return wait.until(webDriver ->
                    isProductNameMatching(expectedName)
            );

        } catch (TimeoutException e) {

            return false;
        }
    }

    /**
     * Yalnizca tek TL fiyati kabul ederek
     * BigDecimal degerine cevirir.
     */
    private BigDecimal parsePrice(String text) {

        if (text == null) {

            throw new IllegalArgumentException(
                    "Fiyat null olamaz."
            );
        }

        String normalized =
                text.replace('\u00A0', ' ').trim();

        Matcher matcher =
                SINGLE_PRICE_PATTERN.matcher(normalized);

        if (!matcher.matches()) {

            throw new IllegalArgumentException(
                    "Metin tek bir satış fiyatı değil: " +
                            normalized
            );
        }

        String amount = matcher.group(1)
                .replace(".", "")
                .replace(",", ".");

        return new BigDecimal(amount);
    }

    /**
     * Detay ve sepet satis fiyatlarini karsilastirir.
     */
    public boolean isPriceMatching(
            String expectedPrice,
            String actualPrice) {

        if (expectedPrice == null
                || actualPrice == null
                || expectedPrice.isBlank()
                || actualPrice.isBlank()) {

            return false;
        }

        try {

            BigDecimal expected =
                    parsePrice(expectedPrice);

            BigDecimal actual =
                    parsePrice(actualPrice);

            System.out.println(
                    "Beklenen satış fiyatı: " +
                            expectedPrice
            );

            System.out.println(
                    "Sepet satış fiyatı: " +
                            actualPrice
            );

            return expected.compareTo(actual) == 0;

        } catch (IllegalArgumentException e) {

            System.out.println(
                    "Fiyat doğrulama hatası: " +
                            e.getMessage()
            );

            return false;
        }
    }

    /**
     * Sepetteki urun miktarini okur.
     */
    private int getQuantity() {

        WebElement row = getFirstCartRow();

        for (WebElement element :
                row.findElements(quantitySelect)) {

            if (element.isDisplayed()) {

                return extractQuantity(
                        new Select(element)
                                .getFirstSelectedOption()
                                .getText()
                );
            }
        }

        for (WebElement element :
                row.findElements(quantityInput)) {

            if (element.isDisplayed()) {

                return extractQuantity(
                        element.getAttribute("value")
                );
            }
        }

        for (WebElement element :
                row.findElements(quantityText)) {

            if (element.isDisplayed()) {

                int quantity =
                        extractQuantity(element.getText());

                if (quantity >= 0) {
                    return quantity;
                }
            }
        }

        return -1;
    }

    /**
     * Miktar metnindeki ilk tam sayiyi alir.
     */
    private int extractQuantity(String text) {

        if (text == null) {
            return -1;
        }

        Matcher matcher =
                Pattern.compile("\\d+").matcher(text);

        if (matcher.find()) {

            return Integer.parseInt(
                    matcher.group()
            );
        }

        return -1;
    }

    /**
     * Eski metot adi korunur.
     * Miktari degistirmez, sadece dogrular.
     */
    public boolean verifyAndSetQuantityToOne() {

        return hasExactlyOneProductWithQuantityOne();
    }

    /**
     * Sepette tek urun satiri ve
     * bu urunden tam 1 adet bulunmali.
     */
    public boolean hasExactlyOneProductWithQuantityOne() {

        try {

            return wait.until(webDriver -> {

                List<WebElement> rows = getCartRows();

                if (rows.size() != 1) {
                    return false;
                }

                return getQuantity() == 1;
            });

        } catch (TimeoutException e) {

            return false;
        }
    }
}
