package com.enterprise.framework.pages;

import com.enterprise.framework.base.BasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * Page Object for the Login page.
 *
 * <p><b>Design conventions demonstrated here:</b>
 * <ul>
 *   <li>All locators are {@code By} constants or {@code @FindBy} fields — never inline strings</li>
 *   <li>Public methods return {@code this} or the next-page type for method chaining</li>
 *   <li>No assertions inside the Page Object — assertions belong in the test class</li>
 *   <li>All actions delegate to {@link BasePage} wrappers — no raw driver.findElement calls</li>
 *   <li>Error messages are read and returned for assertion in tests</li>
 * </ul>
 *
 * <p><b>Usage in a test:</b>
 * <pre>
 *   LoginPage loginPage = getPage(LoginPage.class);
 *   DashboardPage dashboard = loginPage
 *       .enterUsername("admin@company.com")
 *       .enterPassword("Secure@123")
 *       .clickLogin();
 * </pre>
 */
public class LoginPage extends BasePage {

    // =========================================================================
    // Locators — grouped and named clearly; By constants for non-@FindBy use
    // =========================================================================

    // Input fields
    @FindBy(id = "username")
    private WebElement usernameField;

    @FindBy(id = "password")
    private WebElement passwordField;

    // Buttons
    @FindBy(id = "loginBtn")
    private WebElement loginButton;

    @FindBy(linkText = "Forgot Password?")
    private WebElement forgotPasswordLink;

    @FindBy(id = "ssoLoginBtn")
    private WebElement ssoLoginButton;

    // Feedback elements
    @FindBy(css = ".error-message, .alert-danger")
    private WebElement errorMessage;

    @FindBy(css = ".success-toast, .alert-success")
    private WebElement successMessage;

    @FindBy(css = ".validation-message[data-field='username']")
    private WebElement usernameValidation;

    @FindBy(css = ".validation-message[data-field='password']")
    private WebElement passwordValidation;

    // Structural indicators (for isLoaded)
    private static final By LOGIN_FORM         = By.id("loginForm");
    private static final By LOGIN_LOGO         = By.cssSelector(".login-logo, .brand-logo");
    private static final By ERROR_MSG_LOCATOR  = By.cssSelector(".error-message, .alert-danger");

    // URL fragment expected when this page is active
    private static final String PAGE_URL_FRAGMENT = "/login";

    // =========================================================================
    // Constructor
    // =========================================================================

    public LoginPage(WebDriver driver) {
        super(driver);
    }

    // =========================================================================
    // Page load validation
    // =========================================================================

    @Override
    public boolean isLoaded() {
        try {
            waitForVisible(LOGIN_FORM, 10);
            return getPageUrl().contains(PAGE_URL_FRAGMENT)
                || isDisplayed(LOGIN_FORM);
        } catch (Exception e) {
            log.warn("LoginPage.isLoaded() check failed: {}", e.getMessage());
            return false;
        }
    }

    // =========================================================================
    // Action methods — each returns 'this' or the next page for chaining
    // =========================================================================

    public LoginPage enterUsername(String username) {
        step("Entering username: " + username);
        type(usernameField, username);
        return this;
    }

    public LoginPage enterPassword(String password) {
        step("Entering password");
        type(passwordField, password);
        return this;
    }

    public LoginPage clearUsername() {
        type(usernameField, "");
        return this;
    }

    public LoginPage clearPassword() {
        type(passwordField, "");
        return this;
    }

    /**
     * Clicks the login button and returns the DashboardPage.
     * Use this when you expect a successful login.
     */
    public DashboardPage clickLogin() {
        step("Clicking Login button");
        click(loginButton);
        waitForUrlContains("/dashboard");
        return new DashboardPage(driver);
    }

    /**
     * Clicks the login button without navigating away.
     * Use this when you expect a validation error or failed login.
     */
    public LoginPage clickLoginExpectingError() {
        step("Clicking Login button (expecting failure)");
        click(loginButton);
        return this;
    }

    public ForgotPasswordPage clickForgotPassword() {
        step("Clicking Forgot Password link");
        click(forgotPasswordLink);
        return new ForgotPasswordPage(driver);
    }

    public LoginPage clickSsoLogin() {
        step("Clicking SSO Login");
        click(ssoLoginButton);
        return this;
    }

    public LoginPage pressEnterOnPassword() {
        pressEnter(By.id("password"));
        return this;
    }

    // =========================================================================
    // Combined workflow methods — common login flows as single calls
    // =========================================================================

    /**
     * Full login flow: enters credentials and clicks Login.
     * Use when the test is focused on post-login behavior, not the login page itself.
     */
    public DashboardPage loginAs(String username, String password) {
        return enterUsername(username)
               .enterPassword(password)
               .clickLogin();
    }

    /**
     * Login flow expecting failure — returns this page with error visible.
     */
    public LoginPage loginExpectingFailure(String username, String password) {
        return enterUsername(username)
               .enterPassword(password)
               .clickLoginExpectingError();
    }

    // =========================================================================
    // State readers — used in test assertions
    // =========================================================================

    public String getErrorMessage() {
        waitForVisible(ERROR_MSG_LOCATOR, 5);
        return getText(errorMessage);
    }

    public boolean isErrorMessageDisplayed() {
        return isDisplayed(errorMessage);
    }

    public String getUsernameValidationMessage() {
        return isDisplayed(usernameValidation) ? getText(usernameValidation) : "";
    }

    public String getPasswordValidationMessage() {
        return isDisplayed(passwordValidation) ? getText(passwordValidation) : "";
    }

    public boolean isUsernameFieldEmpty() {
        return getValue(By.id("username")).isEmpty();
    }

    public boolean isPasswordFieldMasked() {
        return "password".equals(getAttribute(By.id("password"), "type"));
    }

    public boolean isForgotPasswordLinkDisplayed() {
        return isDisplayed(forgotPasswordLink);
    }

    public boolean isSsoLoginButtonDisplayed() {
        return isDisplayed(ssoLoginButton);
    }

    public boolean isLoginButtonEnabled() {
        return isEnabled(loginButton);
    }

    public String getPageHeading() {
        return getText(By.cssSelector("h1, .login-title, .page-title"));
    }
}
