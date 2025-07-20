package org.example;

import com.thoughtworks.gauge.AfterScenario;
import com.thoughtworks.gauge.BeforeScenario;
import com.thoughtworks.gauge.Step;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.NoSuchElementException;
import java.util.function.Function;

/**
 * Hepsiburada uçtan uca alışveriş akışı için Selenium 4.15.0 ile uyumlu Step Implementation sınıfı.
 * <p>
 * DİKKAT:
 *  - .spec içindeki step cümleleriyle @Step annotation string'leri HARFİ HARFİNE aynı olmalı.
 *  - Parametreler (<eposta>, <sifre>, <kart_numarasi>, <ay>, <yil>, <cvv>) spec data table'ından (veya row parametresinden) otomatik geçer.
 *  - WebDriverWait artık Selenium 4'te Duration alır.
 */
public class HepsiburadaSteps {

    private WebDriver driver;
    private WebDriverWait wait;
    private final LocatorHelper locatorHelper = new LocatorHelper();

    // GLOBAL AYARLAR
    private static final Duration PAGE_LOAD_TIMEOUT = Duration.ofSeconds(40);
    private static final Duration IMPLICIT_WAIT = Duration.ofSeconds(0); // Bilerek 0 (explicit wait kullanıyoruz)
    private static final Duration EXPLICIT_WAIT = Duration.ofSeconds(25);
    private static final Duration POLLING_INTERVAL = Duration.ofMillis(400);

    @BeforeScenario
    public void setup() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        // İstersen headless açmak için yorum satırını kaldır:
        // options.addArguments("--headless=new");
        options.addArguments("--disable-notifications", "--start-maximized", "--no-sandbox", "--disable-gpu");
        driver = new ChromeDriver(options);
        driver.manage().timeouts().pageLoadTimeout(PAGE_LOAD_TIMEOUT);
        driver.manage().timeouts().implicitlyWait(IMPLICIT_WAIT);
        wait = new WebDriverWait(driver, EXPLICIT_WAIT);
        wait.pollingEvery(POLLING_INTERVAL);
    }

    @AfterScenario
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    // ===================== YARDIMCI METODLAR =====================
    private WebElement waitClickable(By by) {
        return wait.until(ExpectedConditions.elementToBeClickable(by));
    }

    private WebElement waitVisible(By by) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(by));
    }

    private void safeClick(By by) {
        WebElement el = waitClickable(by);
        try {
            el.click();
        } catch (ElementClickInterceptedException e) {
            // alternatif: JS click
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", el);
        }
    }

    private void clearAndType(By by, String value) {
        WebElement input = waitVisible(by);
        try {
            input.clear();
        } catch (InvalidElementStateException ignore) {
            // bazı custom inputlarda clear çalışmayabilir; fallback
            input.sendKeys(Keys.chord(Keys.CONTROL, "a"));
            input.sendKeys(Keys.DELETE);
        }
        input.sendKeys(value);
    }

    private boolean exists(By by, Duration timeout) {
        try {
            new WebDriverWait(driver, timeout).until(d -> driver.findElement(by));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void dismissCookieIfPresent() {
        By cookieBtn = By.id("onetrust-accept-btn-handler");
        if (exists(cookieBtn, Duration.ofSeconds(5))) {
            try { safeClick(cookieBtn); } catch (Exception ignore) {}
        }
    }

    // ===================== SPEC STEPLERİ =====================


    @Step("Hepsiburada ana sayfasına gidilir")
    public void navigateToHepsiburadaHomePage() {
        driver.get("https://www.hepsiburada.com/");
        dismissCookieIfPresent();
    }

    @Step("Ana sayfadaki 3. ürüne girilir")
    public void clickThirdProductOnHomePage() {
        // third_product_image locator'ının ürün linkini veya img'sini temsil ettiğini varsayıyoruz.
        safeClick(locatorHelper.getLocator("third_product_image"));
        // Yeni sekme açıldıysa ona geç (Hepsiburada bazı ürünlerde yeni tab açabiliyor)
        switchToLastWindow();
    }

    @Step("Ürün sepete eklenir")
    public void addProductToCart() {
        safeClick(locatorHelper.getLocator("add_to_cart_button"));
    }

    @Step("Sepete gidilir")
    public void navigateToCart() {
        safeClick(locatorHelper.getLocator("shopping_cart_button"));
    }

    @Step("Alışverişi tamamla butonuna tıklanılır")
    public void clickCompleteShoppingButton() {
        safeClick(locatorHelper.getLocator("complete_shopping_button"));
    }

    @Step("Üye girişi ekranında email adresi <eposta> girilir")
    public void enterEmailOnLoginPage(String eposta) {
        clearAndType(locatorHelper.getLocator("login_email_input"), eposta);
    }

    @Step("Şifre <sifre> girilir")
    public void enterPasswordOnLoginPage(String sifre) {
        clearAndType(locatorHelper.getLocator("login_password_input"), sifre);
    }

    @Step("Giriş yap butonuna tıklanılır")
    public void clickLoginButton() {
        safeClick(locatorHelper.getLocator("login_button"));
    }

    @Step("Kart bilgilerini gir butonuna tıklanılır")
    public void clickEnterCardDetailsButton() {
        safeClick(locatorHelper.getLocator("enter_card_details_button"));
    }

    @Step("Kart numarası <kart_numarasi> girilir")
    public void enterCardNumber(String kartNumarasi) {
        clearAndType(locatorHelper.getLocator("card_number_input"), kartNumarasi);
    }

    @Step("Ay/Yıl <ay> <yil> girilir")
    public void enterExpiryDate(String ay, String yil) {
        // Tek inline input varsayımı (MM/YY). Eğer ayrı alan ise locatorları ayır.
        String yearPart = (yil != null && yil.length() > 2) ? yil.substring(yil.length() - 2) : yil;
        clearAndType(locatorHelper.getLocator("card_expiry_date_input"), ay + "/" + yearPart);
    }

    @Step("CVV <cvv> girilir")
    public void enterCvv(String cvv) {
        clearAndType(locatorHelper.getLocator("card_cvv_input"), cvv);
    }

    @Step("Kart kaydet boxı işaretlenir")
    public void checkSaveCardCheckbox() {
        toggleCheckbox(locatorHelper.getLocator("save_card_checkbox"), true);
    }

    @Step("Sözleşme boxı işaretlenir")
    public void checkContractCheckbox() {
        toggleCheckbox(locatorHelper.getLocator("contract_checkbox"), true);
    }

    // ===================== EK YARDIMCI METODLAR =====================

    private void toggleCheckbox(By by, boolean shouldBeSelected) {
        WebElement cb = waitClickable(by);
        boolean selected;
        try {
            selected = cb.isSelected();
        } catch (StaleElementReferenceException e) {
            cb = waitClickable(by);
            selected = cb.isSelected();
        }
        if (selected != shouldBeSelected) {
            cb.click();
        }
    }

    private void switchToLastWindow() {
        String lastHandle = driver.getWindowHandle();
        for (String handle : driver.getWindowHandles()) {
            lastHandle = handle; // döngü sonunda son eleman
        }
        driver.switchTo().window(lastHandle);
    }

    // (Opsiyonel) iFrame kart alanı durumunda kullanım için örnek:
    private void switchIntoFrameIfExists(String frameCssOrId) {
        try {
            driver.switchTo().defaultContent();
            // önce id ile dene yoksa css selector
            try {
                driver.switchTo().frame(frameCssOrId);
                return;
            } catch (NoSuchFrameException ignored) {}
            WebElement frameEl = driver.findElement(By.cssSelector(frameCssOrId));
            driver.switchTo().frame(frameEl);
        } catch (Exception ignored) {
            // frame yoksa sessiz geçiyoruz
        }
    }
}
