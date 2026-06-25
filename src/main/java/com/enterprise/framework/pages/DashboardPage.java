package com.enterprise.framework.pages;

import com.enterprise.framework.base.BasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * Page Object for the Dashboard (home page after login).
 *
 * <p>Demonstrates a page with a navigation bar component and multiple content sections.
 * The navigation bar is composed as an inner object to share it across pages that
 * embed the same nav (DashboardPage, ReportsPage, SettingsPage, etc.).
 */
public class DashboardPage extends BasePage {

    // =========================================================================
    // Locators
    // =========================================================================

    private static final By PAGE_HEADER          = By.cssSelector(".dashboard-header, h1.page-title");
    private static final By WELCOME_MESSAGE      = By.cssSelector(".welcome-message, .greeting");
    private static final By USER_AVATAR_MENU     = By.cssSelector(".user-avatar, .user-profile-btn");
    private static final By LOGOUT_MENU_ITEM     = By.cssSelector("[data-action='logout'], .logout-btn");
    private static final By NOTIFICATION_BELL    = By.cssSelector(".notification-bell, .notification-icon");
    private static final By NOTIFICATION_COUNT   = By.cssSelector(".notification-count, .badge-count");
    private static final By LOADING_SPINNER      = By.cssSelector(".loading-spinner, .page-loader");
    private static final By SIDEBAR_NAV          = By.cssSelector(".sidebar, .main-nav");

    // Page URL fragment
    private static final String URL_FRAGMENT = "/dashboard";

    @FindBy(css = ".user-display-name, .current-user-name")
    private WebElement userDisplayName;

    @FindBy(css = ".stats-card")
    private java.util.List<WebElement> statCards;

    // =========================================================================
    // Constructor
    // =========================================================================

    public DashboardPage(WebDriver driver) {
        super(driver);
        // Wait for the spinner to disappear before asserting load
        try { waitForInvisible(LOADING_SPINNER); } catch (Exception ignored) { }
    }

    // =========================================================================
    // Page load validation
    // =========================================================================

    @Override
    public boolean isLoaded() {
        try {
            return getPageUrl().contains(URL_FRAGMENT)
                && isPresent(PAGE_HEADER)
                && isPresent(SIDEBAR_NAV);
        } catch (Exception e) {
            return false;
        }
    }

    // =========================================================================
    // Navigation actions
    // =========================================================================

    public LoginPage logout() {
        step("Logging out");
        click(USER_AVATAR_MENU);
        waitForVisible(LOGOUT_MENU_ITEM);
        click(LOGOUT_MENU_ITEM);
        waitForUrlContains("/login");
        return new LoginPage(driver);
    }

    public NavigationBar getNavBar() {
        return new NavigationBar(driver);
    }

    // =========================================================================
    // State readers
    // =========================================================================

    public String getWelcomeMessage() {
        return isPresent(WELCOME_MESSAGE) ? getText(WELCOME_MESSAGE) : "";
    }

    public String getLoggedInUsername() {
        return getText(userDisplayName);
    }

    public int getNotificationCount() {
        if (!isPresent(NOTIFICATION_COUNT)) return 0;
        try {
            return Integer.parseInt(getText(NOTIFICATION_COUNT));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public int getStatCardCount() {
        return statCards.size();
    }

    public boolean isWelcomeMessageDisplayed() {
        return isPresent(WELCOME_MESSAGE) && isDisplayed(WELCOME_MESSAGE);
    }

    public String getPageHeading() {
        return isPresent(PAGE_HEADER) ? getText(PAGE_HEADER) : "";
    }
}
