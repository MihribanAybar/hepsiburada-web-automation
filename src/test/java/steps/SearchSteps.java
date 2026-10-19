package steps;

import com.thoughtworks.gauge.Step;
import com.thoughtworks.gauge.datastore.ScenarioDataStore;
import org.openqa.selenium.WebElement;
import pages.HomePage;
import pages.SearchResultsPage;
import support.DriverFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SearchSteps: Ürün arama, arama sonuçlarını doğrulama
 * ve ikinci satırdaki ilk ürünü seçme adımlarını yönetir.
 *
 * Ürün seçiminde sabit indeks kullanılmaz.
 * Responsive ekran düzenine göre dinamik seçim yapılır.
 */
public class SearchSteps {

    private HomePage homePage;
    private SearchResultsPage searchPage;

    /**
     * HomePage nesnesini oluşturur ve tekrar kullanır.
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
     * SearchResultsPage nesnesini oluşturur
     * ve tekrar kullanır.
     */
    private SearchResultsPage getSearchPage() {

        if (searchPage == null) {
            searchPage = new SearchResultsPage(
                    DriverFactory.getDriver()
            );
        }

        return searchPage;
    }

    /**
     * Arama kutusuna verilen kelimeyi yazar
     * ve Enter ile aramayı başlatır.
     */
    @Step("Arama kutusuna <keyword> yaz ve ara")
    public void searchProduct(String keyword) {

        assertThat(keyword)
                .as("Arama kelimesi boş olmamalıdır.")
                .isNotBlank();

        // Aranan kelimeyi senaryo boyunca sakla.
        ScenarioDataStore.put(
                "searchKeyword",
                keyword
        );

        getHomePage().search(keyword);

        System.out.println(
                "Aranan kelime: " + keyword
        );
    }

    /**
     * Arama sonuçlarının yüklendiğini ve
     * ürün listesinin görüntülendiğini doğrular.
     */
    @Step("Arama sonuçlarının yüklendiğini ve ürün listesinin görünür olduğunu doğrula")
    public void verifySearchResults() {

        String keyword = (String)
                ScenarioDataStore.get("searchKeyword");

        assertThat(keyword)
                .as("Önce arama işlemi yapılmış olmalıdır.")
                .isNotBlank();

        assertThat(
                getSearchPage().isSearchResultsDisplayed(keyword)
        )
                .as(
                        "'%s' aramasının sonuçları yüklenmeli " +
                                "ve ürün listesi görünür olmalıdır.",
                        keyword
                )
                .isTrue();

        System.out.println(
                "Arama sonuçları doğrulandı: " + keyword
        );
    }

    /**
     * Ürünlerin ekrandaki X ve Y koordinatlarına
     * göre ikinci satırdaki ilk ürünü bulur.
     *
     * Sabit sütun sayısı veya indeks kullanılmaz.
     */
    @Step("İkinci satırdaki ilk ürünü seç")
    public void selectFirstProductOfSecondRow() {

        // Responsive tasarıma uygun dinamik ürün seçimi
        WebElement product = getSearchPage()
                .getFirstProductOfSecondRow();

        assertThat(product)
                .as("İkinci satırdaki ilk ürün bulunmalıdır.")
                .isNotNull();

        System.out.println(
                "İkinci satırdaki ilk ürün seçiliyor."
        );

        // Ürün detay sayfasına git.
        getSearchPage().clickProduct(product);

        System.out.println(
                "Ürün detay sayfasına geçiş gerçekleştirildi."
        );
    }
}
