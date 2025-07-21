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
        try {
            System.out.println("1. 'setup' metodu başladı.");

            System.out.println("2. WebDriverManager cache temizleniyor ve driver kuruluyor...");
            // WebDriverManager.chromedriver().clearDriverCache().setup(); // Önce cache'i temizlemeden deneyelim
            WebDriverManager.chromedriver().setup();
            System.out.println("3. WebDriverManager başarıyla tamamlandı.");

            System.out.println("4. ChromeOptions oluşturuluyor...");
            ChromeOptions options = new ChromeOptions();
            options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
            options.addArguments("--disable-blink-features=AutomationControlled");
            options.addArguments("--disable-notifications", "--start-maximized", "--no-sandbox", "--disable-gpu");
            options.addArguments("--remote-allow-origins=*");
            System.out.println("5. ChromeOptions başarıyla oluşturuldu.");

            System.out.println("6. ChromeDriver (TARAYICI) başlatılıyor. Bu adımdan sonra tarayıcı açılmalı...");
            driver = new ChromeDriver(options);
            System.out.println("7. ChromeDriver başarıyla başlatıldı.");

            driver.manage().timeouts().pageLoadTimeout(PAGE_LOAD_TIMEOUT);
            driver.manage().timeouts().implicitlyWait(IMPLICIT_WAIT);
            wait = new WebDriverWait(driver, EXPLICIT_WAIT);
            wait.pollingEvery(POLLING_INTERVAL);
            System.out.println("8. Wait ayarları yapıldı. Setup tamamlandı.");

        } catch (Exception e) {
            System.err.println("!!!!!!!! SETUP METODUNDA HATA OLUŞTU !!!!!!!!");
            e.printStackTrace();
        }
    }

    @AfterScenario
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    // ===================== YARDIMCI METODLAR (Senin Kodun) =====================
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
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", el);
        }
    }

    private void clearAndType(By by, String value) {
        WebElement input = waitVisible(by);
        try {
            input.clear();
            input.sendKeys(value);
        } catch (InvalidElementStateException ignore) {
            input.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.DELETE);
            input.sendKeys(value);
        }
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

    private void toggleCheckbox(By by, boolean shouldBeSelected) {
        WebElement cb = waitClickable(by);
        boolean selected = cb.isSelected();
        if (selected != shouldBeSelected) {
            cb.click();
        }
    }

    private void switchToLastWindow() {
        String lastHandle = driver.getWindowHandle();
        for (String handle : driver.getWindowHandles()) {
            lastHandle = handle;
        }
        driver.switchTo().window(lastHandle);
    }

    // ===================== SPEC STEPLERİ (Entegre Edilmiş) =====================

    @Step("Hepsiburada ana sayfasına gidilir")
    public void navigateToHepsiburadaHomePage() {
        driver.get("https://www.hepsiburada.com/");
        dismissCookieIfPresent();
    }

    @Step("Arama kutusuna <kelime> yazılır")
    public void searchForItem(String kelime) {
        // Artık sağlam clearAndType metodunu kullanıyoruz.
        clearAndType(locatorHelper.getLocator("arama_kutusu"), kelime);
        // Enter tuşuna basarak arama yapmak için
        waitVisible(locatorHelper.getLocator("arama_kutusu")).sendKeys(Keys.ENTER);
        System.out.println("'" + kelime + "' arandı.");
    }

    @Step("Çıkan sonuçlardan 3. ürüne gidilir")
    public void clickThirdProduct() {
        // Artık sağlam safeClick metodunu kullanıyoruz.
        safeClick(locatorHelper.getLocator("arama_sonucu_ucuncu_urun"));
        // Yeni sekme açılma ihtimaline karşı son sekmeye geçiş yapıyoruz.
        switchToLastWindow();
        System.out.println("Arama sonuçlarındaki 3. ürüne tıklandı.");
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
        String yearPart = (yil != null && yil.length() > 2) ? yil.substring(yil.length() - 2) : yil;
        clearAndType(locatorHelper.getLocator("card_expiry_date_input"), ay + yearPart);
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
}