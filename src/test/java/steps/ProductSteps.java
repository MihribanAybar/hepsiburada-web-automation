package steps;

import com.thoughtworks.gauge.Step;
import com.thoughtworks.gauge.datastore.ScenarioDataStore;
import pages.ProductPage;
import support.DriverFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ProductSteps: Ürün detay sayfasına ait Gauge adımlarını yönetir.
 *
 * Ürün sayfasını doğrular, ürün bilgilerini kaydeder,
 * Sepete Ekle butonunu kontrol eder ve ürünü sepete ekler.
 */
public class ProductSteps {

    private ProductPage productPage;

    /**
     * ProductPage nesnesini oluşturur ve tekrar kullanır.
     */
    private ProductPage getProductPage() {

        if (productPage == null) {
            productPage = new ProductPage(
                    DriverFactory.getDriver()
            );
        }

        return productPage;
    }

    /**
     * Seçilen ürünün detay sayfasına yönlenildiğini doğrular.
     */
    @Step("İkinci satırdaki ilk ürünün sayfasına yönlendiğini doğrula")
    public void verifyNavigatedToProductPage() {

        assertThat(
                getProductPage().isOnProductPage()
        )
                .as("Seçilen ürünün detay sayfası açılmalıdır.")
                .isTrue();

        System.out.println(
                "Ürün detay sayfası doğrulandı."
        );
    }

    /**
     * Ürün adını kaydeder.
     * Daha sonra sepet doğrulamasında kullanılacaktır.
     */
    @Step("Ürün adını kaydet")
    public void saveProductName() {

        String title = getProductPage().getProductTitle();

        assertThat(title)
                .as("Ürün detay sayfasındaki ürün adı boş olmamalıdır.")
                .isNotBlank();

        ScenarioDataStore.put(
                "selectedProductName",
                title
        );

        System.out.println(
                "Kaydedilen ürün adı: " + title
        );
    }

    /**
     * Ürün fiyatını kaydeder.
     * Fiyat, sepet ekranındaki fiyatla karşılaştırılacaktır.
     */
    @Step("Ürün fiyatını kaydet")
    public void saveProductPrice() {

        String price = getProductPage().getProductPrice();

        assertThat(price)
                .as("Ürün detay sayfasındaki fiyat okunabilmelidir.")
                .isNotBlank();

        ScenarioDataStore.put(
                "selectedProductPrice",
                price
        );

        System.out.println(
                "Ürün fiyatı kaydedildi: " + price
        );
    }

    /**
     * Sepete Ekle butonunun aktif olduğunu doğrular.
     */
    @Step("Sepete Ekle butonunun aktif olduğunu doğrula")
    public void verifyAddToCartButtonEnabled() {

        assertThat(
                getProductPage().isAddToCartButtonEnabled()
        )
                .as("Sepete Ekle butonu aktif ve tıklanabilir olmalıdır.")
                .isTrue();

        System.out.println(
                "Sepete Ekle butonu aktif."
        );
    }

    /**
     * Ürünü sepete ekler.
     */
    @Step("Sepete Ekle butonuna tıkla")
    public void clickAddToCart() {

        getProductPage().addToCart();

        System.out.println(
                "Ürün sepete ekleme işlemi başlatıldı."
        );
    }

    /**
     * Sepete ekleme onay mesajını doğrular.
     */
    @Step("Sepete eklendiğine dair onay mesajını gör")
    public void verifyAddToCartConfirmation() {

        assertThat(
                getProductPage()
                        .isAddedToCartConfirmationDisplayed()
        )
                .as("Ürünün sepete eklendiğine dair onay mesajı görünmelidir.")
                .isTrue();

        System.out.println(
                "Sepete ekleme onayı doğrulandı."
        );
    }
}
