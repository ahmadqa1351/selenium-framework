package com.enterprise.framework.pages;

import com.enterprise.framework.base.BasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * Reusable navigation bar component shared across multiple pages.
 *
 * <p>Instead of duplicating nav locators and methods in every page that has a sidebar,
 * this component is composed into page objects that need it. This is the Component
 * Object Model (COM) pattern — a natural extension of POM for shared UI regions.
 *
 * <p><b>Usage in a page:</b>
 * <pre>
 *   public NavigationBar getNavBar() {
 *       return new NavigationBar(driver);
 *   }
 * </pre>
 *
 * <p><b>Usage in a test:</b>
 * <pre>
 *   SettingsPage settings = dashboardPage.getNavBar().goToSettings();
 * </pre>
 */
public class NavigationBar extends BasePage {

    private static final By NAV_SETTINGS     = By.cssSelector("[data-nav='settings'], a[href*='/settings']");
    private static final By NAV_USERS        = By.cssSelector("[data-nav='users'], a[href*='/users']");
    private static final By NAV_REPORTS      = By.cssSelector("[data-nav='reports'], a[href*='/reports']");
    private static final By NAV_DASHBOARD    = By.cssSelector("[data-nav='dashboard'], a[href*='/dashboard']");
    private static final By NAV_PROFILE      = By.cssSelector("[data-nav='profile'], a[href*='/profile']");

    public NavigationBar(WebDriver driver) {
        super(driver);
    }

    @Override
    public boolean isLoaded() {
        return isPresent(NAV_DASHBOARD);
    }

    public DashboardPage goToDashboard() {
        step("Navigating to Dashboard via sidebar");
        click(NAV_DASHBOARD);
        waitForUrlContains("/dashboard");
        return new DashboardPage(driver);
    }

    public boolean isSettingsLinkVisible() { return isDisplayed(NAV_SETTINGS); }
    public boolean isUsersLinkVisible()    { return isDisplayed(NAV_USERS); }
    public boolean isReportsLinkVisible()  { return isDisplayed(NAV_REPORTS); }
}


// ─────────────────────────────────────────────────────────────────────────────
// Stub page objects referenced from LoginPage
// ─────────────────────────────────────────────────────────────────────────────

class ForgotPasswordPage extends BasePage {

    @FindBy(id = "resetEmail")
    private WebElement emailField;

    @FindBy(css = ".submit-btn, #resetBtn")
    private WebElement submitButton;

    @FindBy(css = ".success-message, .confirmation-text")
    private WebElement confirmationText;

    private static final By FORM_LOCATOR = By.cssSelector(".forgot-password-form, #resetForm");

    public ForgotPasswordPage(WebDriver driver) {
        super(driver);
    }

    @Override
    public boolean isLoaded() {
        return isPresent(FORM_LOCATOR) || getPageUrl().contains("/forgot-password");
    }

    public ForgotPasswordPage enterEmail(String email) {
        step("Entering reset email: " + email);
        type(emailField, email);
        return this;
    }

    public ForgotPasswordPage clickSubmit() {
        step("Submitting forgot password form");
        click(submitButton);
        return this;
    }

    public String getConfirmationMessage() {
        waitForVisible(By.cssSelector(".success-message, .confirmation-text"), 10);
        return getText(confirmationText);
    }

    public boolean isConfirmationDisplayed() {
        return isPresent(By.cssSelector(".success-message, .confirmation-text"));
    }

    public LoginPage goBackToLogin() {
        driver.navigate().back();
        return new LoginPage(driver);
    }
}
