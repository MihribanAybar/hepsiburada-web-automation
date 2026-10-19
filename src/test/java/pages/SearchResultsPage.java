
package pages;

import org.openqa.selenium.By;
import support.LocatorReader;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * SearchResultsPage:
 * Hepsiburada arama sonuçları sayfasını temsil eder.
 *
 * Ana ürün listesinin CSS Grid kolon sayısını
 * dinamik okuyarak ikinci satırın ilk ürününü seçer.
 *
 * Sabit kolon sayısı kullanılmaz.
 * Thread.sleep kullanılmaz.
 */
public class SearchResultsPage {

    private final WebDriver driver;
    private final WebDriverWait wait;

    private static final int WAIT_SECONDS = 20;

    /**
     * Kullanıcının Chrome DevTools üzerinden
     * doğruladığı gerçek grid locator'ı.
     */
    private final By productListContainer =
            LocatorReader.getLocator("Shopping/search.json", "productListContainer");

    /**
     * Grid'in doğrudan altındaki ürün kartları.
     *
     * Reklam carousel'leri ve sayfadaki diğer
     * ürün kartları bu kapsamın dışında kalır.
     */
    private final By directProductItems =
            LocatorReader.getLocator("Shopping/search.json", "directProductItems");

    /**
     * Ürün detay sayfasına giden bağlantılar.
     */
    private final By productLinks =
            LocatorReader.getLocator("Shopping/search.json", "productLinks");

    public SearchResultsPage(WebDriver driver) {

        this.driver = driver;

        this.wait = new WebDriverWait(
                driver,
                Duration.ofSeconds(WAIT_SECONDS)
        );

        this.wait.pollingEvery(
                Duration.ofMillis(500)
        );
    }

    /**
     * Görünür ana ürün grid'ini bulur.
     *
     * Bekleme içermez.
     * WebDriverWait koşullarında kullanılır.
     */
    private WebElement findProductGridNow() {

        List<WebElement> grids =
                driver.findElements(productListContainer);

        for (WebElement grid : grids) {

            try {

                if (grid.isDisplayed()) {
                    return grid;
                }

            } catch (StaleElementReferenceException ignored) {
                // Sayfa güncellenmiş olabilir.
            }
        }

        return null;
    }

    /**
     * Grid'in anlık kolon sayısını CSS üzerinden okur.
     *
     * Chrome DevTools'ta doğrulanan yöntem:
     *
     * window.getComputedStyle(container)
     *       .gridTemplateColumns
     *
     * CSS Grid değilse yanlış indeksle seçim yapmaz.
     */
    private int getColumnCount(WebElement grid) {

        JavascriptExecutor js =
                (JavascriptExecutor) driver;

        String display = (String) js.executeScript(
                "return window.getComputedStyle(arguments[0]).display;",
                grid
        );

        if (!"grid".equals(display)
                && !"inline-grid".equals(display)) {

            throw new IllegalStateException(
                    "Ürün listesi CSS Grid değil. " +
                            "Bulunan display değeri: " + display
            );
        }

        String gridTemplateColumns =
                (String) js.executeScript(
                        "return window.getComputedStyle(" +
                                "arguments[0]).gridTemplateColumns;",
                        grid
                );

        if (gridTemplateColumns == null
                || gridTemplateColumns.isBlank()
                || gridTemplateColumns.equals("none")) {

            throw new IllegalStateException(
                    "CSS Grid kolon bilgisi okunamadı."
            );
        }

        int columnCount =
                gridTemplateColumns
                        .trim()
                        .split("\\s+")
                        .length;

        if (columnCount < 1) {

            throw new IllegalStateException(
                    "Geçerli kolon sayısı bulunamadı."
            );
        }

        return columnCount;
    }

    /**
     * Sadece ana grid'in doğrudan li çocuklarını alır.
     *
     * Bütün sayfadaki productCard div'lerini
     * taramaz.
     */
    private List<WebElement> getGridProducts(
            WebElement grid) {

        return grid.findElements(directProductItems);
    }

    /**
     * Ürün kartının içindeki gerçek detay
     * bağlantısını bulur.
     *
     * Sepete ekle gibi başka aksiyonlara tıklamaz.
     *
     * Uygun bağlantı henüz yoksa null döner.
     */
    private WebElement findProductDetailLink(
            WebElement productCard) {

        List<WebElement> links =
                productCard.findElements(productLinks);

        for (WebElement link : links) {

            try {

                String href =
                        link.getAttribute("href");

                if (href == null || href.isBlank()) {
                    continue;
                }

                boolean isProductUrl =
                        href.contains("-p-")
                                || href.contains("/p/");

                if (isProductUrl
                        && link.isDisplayed()
                        && link.isEnabled()) {

                    return link;
                }

            } catch (StaleElementReferenceException ignored) {
                // Ürün kartı yeniden oluşturulmuş olabilir.
            }
        }

        return null;
    }

    /**
     * Grid'in en az iki ürün satırı içerdiğini
     * kontrol eder.
     *
     * Bu metot WebDriverWait içinde çağrılır.
     * İçerisinde ikinci bir wait yoktur.
     */
    private boolean areTwoRowsReadyNow() {

        try {

            WebElement grid = findProductGridNow();

            if (grid == null) {
                return false;
            }

            int columnCount = getColumnCount(grid);

            List<WebElement> products =
                    getGridProducts(grid);

            // 4 kolon varsa en az 5 ürün,
            // 3 kolon varsa en az 4 ürün gerekir.
            if (products.size() <= columnCount) {
                return false;
            }

            // İlk ürün kartının gerçek bir ürün
            // bağlantısı içerdiğini kontrol et.
            if (findProductDetailLink(
                    products.get(0)
            ) == null) {
                return false;
            }

            // İkinci satırın ilk ürünü de
            // detay bağlantısına sahip olmalı.
            WebElement target =
                    products.get(columnCount);

            return target.isDisplayed()
                    && findProductDetailLink(target) != null;

        } catch (StaleElementReferenceException e) {

            return false;
        }
    }

    /**
     * Ana grid'in ve ikinci satırın
     * yüklenmesini en fazla 20 saniye bekler.
     */
    public void waitForProducts() {

        try {

            wait.withMessage(
                    "20 saniyede CSS Grid üzerinde " +
                            "ikinci satırın ilk ürünü bulunamadı."
            ).until(webDriver ->
                    areTwoRowsReadyNow()
            );

        } catch (TimeoutException e) {

            throw new TimeoutException(
                    "Arama sonuçları beklenen sürede " +
                            "yüklenmedi. Ana ürün grid'ini " +
                            "ve doğrudan li çocuklarını kontrol edin.",
                    e
            );
        }
    }

    /**
     * Arama sonuçlarının açıldığını doğrular.
     */
    public boolean isSearchResultsDisplayed(
            String keyword) {

        if (keyword == null || keyword.isBlank()) {
            return false;
        }

        String currentUrl =
                driver.getCurrentUrl().toLowerCase();

        if (!currentUrl.contains(
                keyword.toLowerCase())) {

            return false;
        }

        try {

            waitForProducts();

            System.out.println(
                    "Arama sonuçları grid'i yüklendi."
            );

            return true;

        } catch (TimeoutException e) {

            System.out.println(
                    "Arama sonuçları yüklenemedi: " +
                            e.getMessage()
            );

            return false;
        }
    }

    /**
     * İkinci satırın ilk ürününü
     * CSS Grid kolon sayısıyla bulur.
     *
     * 3 kolon -> index 3
     * 4 kolon -> index 4
     * 5 kolon -> index 5
     */
    public WebElement getFirstProductOfSecondRow() {

        waitForProducts();

        try {

            return wait.withMessage(
                    "İkinci satırın ilk ürünü " +
                            "20 saniyede belirlenemedi."
            ).until(webDriver -> {

                try {

                    WebElement grid =
                            findProductGridNow();

                    if (grid == null) {
                        return null;
                    }

                    int columnCount =
                            getColumnCount(grid);

                    List<WebElement> products =
                            getGridProducts(grid);

                    /*
                     * 2. satırın 1. ürünü:
                     *
                     * targetIndex =
                     * (2 - 1) * columnCount + (1 - 1)
                     *
                     * targetIndex = columnCount
                     */
                    int targetIndex = columnCount;

                    if (products.size() <= targetIndex) {
                        return null;
                    }

                    WebElement target =
                            products.get(targetIndex);

                    if (!target.isDisplayed()) {
                        return null;
                    }

                    if (findProductDetailLink(target)
                            == null) {
                        return null;
                    }

                    System.out.println(
                            "CSS Grid kolon sayısı: " +
                                    columnCount
                    );

                    System.out.println(
                            "Seçilecek ürün indeksi: " +
                                    targetIndex
                    );

                    System.out.println(
                            "İkinci satırdaki ilk ürün bulundu."
                    );

                    return target;

                } catch (StaleElementReferenceException e) {

                    return null;
                }
            });

        } catch (TimeoutException e) {

            throw new TimeoutException(
                    "İkinci satırın ilk ürünü " +
                            "CSS Grid yöntemiyle bulunamadı.",
                    e
            );
        }
    }

    /**
     * Eski SearchSteps kullanımıyla
     * geriye dönük uyumluluk sağlar.
     *
     * Gönderilen kolon sayısı kullanılmaz.
     * Gerçek sayı CSS üzerinden okunur.
     */
    public WebElement getFirstProductOfSecondRow(
            int columnsPerRow) {

        if (columnsPerRow < 1) {

            throw new IllegalArgumentException(
                    "Kolon sayısı pozitif olmalıdır."
            );
        }

        return getFirstProductOfSecondRow();
    }

    /**
     * Ürün kartının başlık bilgisini döndürür.
     */
    public String getProductName(WebElement card) {

        if (card == null) {

            throw new IllegalArgumentException(
                    "Ürün kartı null olamaz."
            );
        }

        WebElement link =
                findProductDetailLink(card);

        if (link == null) {

            throw new IllegalStateException(
                    "Ürün detay bağlantısı bulunamadı."
            );
        }

        String title = link.getAttribute("title");

        if (title != null && !title.isBlank()) {
            return title.trim();
        }

        return link.getText().trim();
    }

    /**
     * Seçilen ürünün gerçek bağlantısına tıklar.
     *
     * Yeni sekme açılırsa geçer.
     * Aynı sekmede açılırsa mevcut sekmede devam eder.
     */
    public void clickProduct(WebElement card) {

        if (card == null) {

            throw new IllegalArgumentException(
                    "Tıklanacak ürün kartı null olamaz."
            );
        }

        WebElement productLink =
                findProductDetailLink(card);

        if (productLink == null) {

            throw new IllegalStateException(
                    "Ürün detay sayfasına giden " +
                            "bağlantı bulunamadı."
            );
        }

        String originalUrl =
                driver.getCurrentUrl();

        String originalWindow =
                driver.getWindowHandle();

        Set<String> originalWindows =
                new HashSet<>(
                        driver.getWindowHandles()
                );

        // Gerçek ürün bağlantısını görünür alana kaydır.
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView(" +
                        "{block:'center', inline:'nearest'});",
                productLink
        );

        WebElement clickableLink = wait.until(
                ExpectedConditions.elementToBeClickable(
                        productLink
                )
        );

        // JavaScript click değil, normal Selenium click.
        clickableLink.click();

        // Yeni sekme veya URL değişimini bekle.
        wait.withMessage(
                "Ürün bağlantısına tıklandı fakat " +
                        "20 saniyede ürün sayfasına geçilemedi."
        ).until(webDriver -> {

            Set<String> currentWindows =
                    webDriver.getWindowHandles();

            if (!currentWindows.equals(originalWindows)) {
                return true;
            }

            return !webDriver.getCurrentUrl()
                    .equals(originalUrl);
        });

        Set<String> currentWindows =
                driver.getWindowHandles();

        Set<String> newWindows =
                new HashSet<>(currentWindows);

        newWindows.removeAll(originalWindows);

        if (!newWindows.isEmpty()) {

            driver.switchTo().window(
                    newWindows.iterator().next()
            );

            System.out.println(
                    "Yeni ürün sekmesine geçildi."
            );

        } else {

            driver.switchTo().window(originalWindow);

            System.out.println(
                    "Ürün aynı sekmede açıldı."
            );
        }

        System.out.println(
                "Aktif ürün adresi: " +
                        driver.getCurrentUrl()
        );
    }
}
