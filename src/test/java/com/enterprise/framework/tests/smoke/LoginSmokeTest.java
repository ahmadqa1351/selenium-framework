package com.enterprise.framework.tests.smoke;

import com.enterprise.framework.base.BaseTest;
import com.enterprise.framework.config.EnvironmentConfig;
import com.enterprise.framework.pages.DashboardPage;
import com.enterprise.framework.pages.LoginPage;
import com.enterprise.framework.providers.TestDataProvider;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Map;

/**
 * Smoke tests for the Login feature.
 *
 * <p><b>Purpose:</b> Verify the most critical paths work after every deployment.
 * Smoke tests are fast (&lt;5 min), cover the happy path and one critical failure path,
 * and are the first gate in any CI pipeline.
 *
 * <p><b>Groups:</b> {@code smoke} — included in every pipeline run.
 *
 * <p><b>Annotation strategy:</b>
 * <ul>
 *   <li>{@code priority} controls execution order within the class (lower = first)</li>
 *   <li>{@code description} appears in Extent Reports and TestNG output</li>
 *   <li>{@code retryAnalyzer} is attached globally by {@code TestListener} — no need to set it here</li>
 * </ul>
 */
public class LoginSmokeTest extends BaseTest {

    // =========================================================================
    // TC-SM-001: Valid login — critical path
    // =========================================================================

    @Test(
        groups       = {"smoke", "regression"},
        priority     = 1,
        description  = "TC-SM-001: Verify admin can log in with valid credentials and land on Dashboard"
    )
    public void validAdminLogin() {
        step("Opening Login page");
        LoginPage loginPage = getPage(LoginPage.class);

        step("Verifying Login page is displayed");
        Assert.assertTrue(loginPage.isLoaded(),
            "Login page should be displayed after navigating to base URL");

        step("Entering valid admin credentials");
        DashboardPage dashboard = loginPage.loginAs(
            EnvironmentConfig.getAdminUsername(),
            EnvironmentConfig.getAdminPassword()
        );

        step("Verifying user is on Dashboard");
        Assert.assertTrue(dashboard.isLoaded(),
            "Dashboard should be loaded after successful login");

        stepPass("TC-SM-001 PASSED: Admin logged in successfully and Dashboard is displayed");
    }

    // =========================================================================
    // TC-SM-002: Login page elements visible
    // =========================================================================

    @Test(
        groups      = {"smoke"},
        priority    = 2,
        description = "TC-SM-002: Verify all key login page elements are displayed"
    )
    public void loginPageElementsDisplayed() {
        step("Opening Login page");
        LoginPage loginPage = getPage(LoginPage.class);

        Assert.assertTrue(loginPage.isLoaded(),        "Login form should be visible");
        Assert.assertTrue(loginPage.isForgotPasswordLinkDisplayed(), "Forgot Password link should be visible");
        Assert.assertTrue(loginPage.isPasswordFieldMasked(),          "Password field should be of type=password");
        Assert.assertTrue(loginPage.isLoginButtonEnabled(),           "Login button should be enabled by default");

        stepPass("TC-SM-002 PASSED: All login page elements are visible and in correct state");
    }

    // =========================================================================
    // TC-SM-003: Invalid credentials — error message
    // =========================================================================

    @Test(
        groups      = {"smoke"},
        priority    = 3,
        description = "TC-SM-003: Verify error message is shown for invalid credentials"
    )
    public void invalidCredentialsShowError() {
        step("Opening Login page");
        LoginPage loginPage = getPage(LoginPage.class);

        step("Attempting login with invalid credentials");
        loginPage.loginExpectingFailure("invalid@user.com", "wrongpassword");

        step("Verifying error message is displayed");
        Assert.assertTrue(loginPage.isErrorMessageDisplayed(),
            "Error message should be displayed for invalid credentials");

        String errorMsg = loginPage.getErrorMessage();
        Assert.assertFalse(errorMsg.isEmpty(), "Error message should not be empty");
        Assert.assertTrue(
            errorMsg.toLowerCase().contains("invalid") ||
            errorMsg.toLowerCase().contains("incorrect") ||
            errorMsg.toLowerCase().contains("wrong"),
            "Error message should indicate invalid credentials. Actual: " + errorMsg
        );

        stepPass("TC-SM-003 PASSED: Error message correctly displayed for invalid credentials");
    }

    // =========================================================================
    // TC-SM-004: Logout flow
    // =========================================================================

    @Test(
        groups      = {"smoke"},
        priority    = 4,
        description = "TC-SM-004: Verify user can log out and is redirected to Login page"
    )
    public void logoutRedirectsToLoginPage() {
        step("Logging in as admin");
        LoginPage loginPage = getPage(LoginPage.class);
        DashboardPage dashboard = loginPage.loginAs(
            EnvironmentConfig.getAdminUsername(),
            EnvironmentConfig.getAdminPassword()
        );

        Assert.assertTrue(dashboard.isLoaded(), "Dashboard should be loaded before logout");

        step("Logging out");
        LoginPage returnedLoginPage = dashboard.logout();

        step("Verifying redirection to Login page");
        Assert.assertTrue(returnedLoginPage.isLoaded(),
            "Should be redirected to Login page after logout");

        Assert.assertTrue(
            returnedLoginPage.getPageUrl().contains("/login"),
            "URL should contain '/login' after logout. Actual: " + returnedLoginPage.getPageUrl()
        );

        stepPass("TC-SM-004 PASSED: User logged out and redirected to Login page");
    }

    // =========================================================================
    // TC-SM-005: Data-driven login from JSON
    // =========================================================================

    @Test(
        groups           = {"smoke", "data-driven"},
        priority         = 5,
        description      = "TC-SM-005: Data-driven valid login from JSON test data file",
        dataProvider     = "validLoginData",
        dataProviderClass = TestDataProvider.class
    )
    public void dataDrivenValidLogin(Map<String, String> testData) {
        String username    = testData.get("username");
        String password    = testData.get("password");
        String expectedRole = testData.getOrDefault("role", "");

        step("Logging in as: " + username + " (role: " + expectedRole + ")");
        LoginPage loginPage = getPage(LoginPage.class);
        DashboardPage dashboard = loginPage.loginAs(username, password);

        Assert.assertTrue(dashboard.isLoaded(),
            "Dashboard should load after login with: " + username);

        if (!expectedRole.isEmpty()) {
            // Optional: verify role-specific welcome message or page content
            String welcomeMsg = dashboard.getWelcomeMessage();
            step("Welcome message: " + welcomeMsg);
        }

        stepPass("Data-driven login passed for: " + username);
    }
}
