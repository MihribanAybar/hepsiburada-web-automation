package steps;

import com.thoughtworks.gauge.Step;
import com.thoughtworks.gauge.datastore.ScenarioDataStore;
import pages.CartPage;
import support.DriverFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CartSteps: Sepet işlemlerinin Gauge adımlarını yönetir.
 *
 * Giriş sonrasında sepeti temizler, boş sepeti doğrular.
 * Test sonunda ürün adını, fiyatını ve miktarını kontrol eder.
 */
public class CartSteps {

    private CartPage cartPage;

    private CartPage getCartPage() {

        if (cartPage == null) {
            cartPage = new CartPage(DriverFactory.getDriver());
        }

        return cartPage;
    }

    /**
     * Header üzerindeki sepet ikonuna tıklar.
     */
    @Step("Header'daki sepet ikonuna tıkla")
    public void openCart() {

        getCartPage().openCart();
    }

    /**
     * Login sonrası önceki testlerden kalan
     * sepet ürünlerini temizler.
     */
    @Step("Sepetteki eski ürünleri temizle")
    public void clearCart() {

        getCartPage().clearCart();

        System.out.println(
                "Sepet temizleme işlemi tamamlandı."
        );
    }

    /**
     * Sepetin gerçekten boş olduğunu doğrular.
     */
    @Step("Sepetin boş olduğunu doğrula")
    public void verifyEmptyCart() {

        assertThat(getCartPage().isCartEmpty())
                .as("Arama işleminden önce sepet boş olmalıdır.")
                .isTrue();

        System.out.println(
                "Sepetin boş olduğu doğrulandı."
        );
    }

    /**
     * Sepetteki ürünün adını, ürün detay sayfasında
     * kaydedilen adla karşılaştırır.
     */
    @Step("Sepette gösterilen ürün adının detay sayfasındakiyle eşleştiğini doğrula")
    public void verifyProductName() {

        String expectedProductName =
                (String) ScenarioDataStore.get(
                        "selectedProductName"
                );

        assertThat(expectedProductName)
                .as("Ürün detay sayfasındaki ürün adı kaydedilmiş olmalıdır.")
                .isNotBlank();

        String actualProductName =
                getCartPage().getCartProductName();

        assertThat(
                getCartPage().isProductNameMatching(
                        expectedProductName
                )
        )
                .as(
                        "Sepetteki ürün adı eşleşmedi. Beklenen: %s, Gerçek: %s",
                        expectedProductName,
                        actualProductName
                )
                .isTrue();

        System.out.println(
                "Sepetteki ürün adı doğrulandı."
        );
    }

    /**
     * Sepet fiyatını detay sayfasında kaydedilen
     * fiyat ile karşılaştırır.
     */
    @Step("Sepetteki ürün fiyatının detay sayfasındakiyle eşleştiğini doğrula")
    public void verifyProductPrice() {

        String expectedPrice =
                (String) ScenarioDataStore.get(
                        "selectedProductPrice"
                );

        assertThat(expectedPrice)
                .as("Ürün detay sayfasındaki fiyat kaydedilmiş olmalıdır.")
                .isNotBlank();

        String cartPrice =
                getCartPage().getCartProductPrice();

        assertThat(cartPrice)
                .as("Sepetteki ürün fiyatı okunabilmelidir.")
                .isNotBlank();

        assertThat(
                getCartPage().isPriceMatching(
                        expectedPrice,
                        cartPrice
                )
        )
                .as(
                        "Fiyatlar eşleşmedi. Detay: %s, Sepet: %s",
                        expectedPrice,
                        cartPrice
                )
                .isTrue();

        System.out.println(
                "Ürün fiyatı doğrulandı."
        );
    }

    /**
     * Eski Gauge adımıyla uyumluluk için korunmuştur.
     * Miktarı değiştirmeden, gerçekten 1 olduğunu doğrular.
     */
    @Step("Sepette ürün adedinin 1 olduğunu doğrula değilse 1 yap")
    public void verifyProductQuantity() {

        assertThat(
                getCartPage().hasExactlyOneProductWithQuantityOne()
        )
                .as(
                        "Sepette yalnızca bir ürün satırı ve bu üründen 1 adet bulunmalıdır."
                )
                .isTrue();

        System.out.println(
                "Sepette tek ürün ve 1 adet olduğu doğrulandı."
        );
    }

    /**
     * Daha doğru isimlendirilmiş yeni Gauge adımı.
     */
    @Step("Sepette yalnızca 1 ürün ve 1 adet olduğunu doğrula")
    public void verifySingleProduct() {

        verifyProductQuantity();
    }

    /**
     * Ürünün sepette bulunduğunu, adının,
     * fiyatının ve miktarının doğru olduğunu doğrular.
     */
    @Step("Eklenen ürünün sepette olduğunu doğrula")
    public void verifyProductInCart() {

        verifyProductName();

        verifyProductPrice();

        verifySingleProduct();

        System.out.println(
                "Sepet doğrulamaları başarıyla tamamlandı."
        );
    }
}
