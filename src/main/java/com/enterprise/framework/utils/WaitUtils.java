package com.enterprise.framework.utils;

import com.enterprise.framework.config.EnvironmentConfig;
import com.enterprise.framework.driver.DriverManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;
import java.util.function.Function;

/**
 * Centralized explicit wait strategies.
 *
 * <p><b>Design philosophy:</b>
 * <ul>
 *   <li>Zero implicit waits — all synchronization goes through this class</li>
 *   <li>All methods are static because they are stateless utilities</li>
 *   <li>Default timeout comes from config; individual callers may override</li>
 *   <li>Fluent waits are used for polling to avoid busy-waiting</li>
 * </ul>
 *
 * <p><b>Why no implicit waits?</b> Mixing implicit and explicit waits creates
 * non-deterministic timing. When they interact, the effective wait time can be
 * the sum of both — making tests slow and their failure timing unpredictable.
 */
public final class WaitUtils {

    private static final Logger log = LogManager.getLogger(WaitUtils.class);
    private static final int DEFAULT_TIMEOUT = EnvironmentConfig.getExplicitWaitSeconds();
    private static final int POLL_INTERVAL_MS = 300;

    private WaitUtils() { }

    // -------------------------------------------------------------------------
    // Core wait builders
    // -------------------------------------------------------------------------

    /** Returns a WebDriverWait using the default configured timeout. */
    public static WebDriverWait defaultWait() {
        return new WebDriverWait(DriverManager.getDriver(), Duration.ofSeconds(DEFAULT_TIMEOUT));
    }

    /** Returns a WebDriverWait with a custom timeout. */
    public static WebDriverWait waitWithTimeout(int seconds) {
        return new WebDriverWait(DriverManager.getDriver(), Duration.ofSeconds(seconds));
    }

    /**
     * Returns a FluentWait that polls every {@code POLL_INTERVAL_MS} milliseconds and
     * silently ignores {@link NoSuchElementException} and {@link StaleElementReferenceException}.
     */
    public static FluentWait<WebDriver> fluentWait(int timeoutSeconds) {
        return new FluentWait<>(DriverManager.getDriver())
            .withTimeout(Duration.ofSeconds(timeoutSeconds))
            .pollingEvery(Duration.ofMillis(POLL_INTERVAL_MS))
            .ignoring(NoSuchElementException.class)
            .ignoring(StaleElementReferenceException.class);
    }

    // -------------------------------------------------------------------------
    // Element visibility
    // -------------------------------------------------------------------------

    public static WebElement waitForVisible(By locator) {
        return waitForVisible(locator, DEFAULT_TIMEOUT);
    }

    public static WebElement waitForVisible(By locator, int seconds) {
        log.debug("Waiting up to {}s for element to be visible: {}", seconds, locator);
        return waitWithTimeout(seconds).until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    public static WebElement waitForVisible(WebElement element) {
        return waitForVisible(element, DEFAULT_TIMEOUT);
    }

    public static WebElement waitForVisible(WebElement element, int seconds) {
        log.debug("Waiting up to {}s for element to be visible", seconds);
        return waitWithTimeout(seconds).until(ExpectedConditions.visibilityOf(element));
    }

    public static List<WebElement> waitForAllVisible(By locator) {
        return defaultWait().until(ExpectedConditions.visibilityOfAllElementsLocatedBy(locator));
    }

    // -------------------------------------------------------------------------
    // Element clickability
    // -------------------------------------------------------------------------

    public static WebElement waitForClickable(By locator) {
        return waitForClickable(locator, DEFAULT_TIMEOUT);
    }

    public static WebElement waitForClickable(By locator, int seconds) {
        log.debug("Waiting up to {}s for element to be clickable: {}", seconds, locator);
        return waitWithTimeout(seconds).until(ExpectedConditions.elementToBeClickable(locator));
    }

    public static WebElement waitForClickable(WebElement element) {
        return waitForClickable(element, DEFAULT_TIMEOUT);
    }

    public static WebElement waitForClickable(WebElement element, int seconds) {
        return waitWithTimeout(seconds).until(ExpectedConditions.elementToBeClickable(element));
    }

    // -------------------------------------------------------------------------
    // Element presence (in DOM, not necessarily visible)
    // -------------------------------------------------------------------------

    public static WebElement waitForPresence(By locator) {
        return defaultWait().until(ExpectedConditions.presenceOfElementLocated(locator));
    }

    public static List<WebElement> waitForPresenceOfAll(By locator) {
        return defaultWait().until(ExpectedConditions.presenceOfAllElementsLocatedBy(locator));
    }

    // -------------------------------------------------------------------------
    // Element disappearance / staleness
    // -------------------------------------------------------------------------

    public static boolean waitForInvisible(By locator) {
        return waitForInvisible(locator, DEFAULT_TIMEOUT);
    }

    public static boolean waitForInvisible(By locator, int seconds) {
        log.debug("Waiting up to {}s for element to be invisible: {}", seconds, locator);
        return waitWithTimeout(seconds).until(ExpectedConditions.invisibilityOfElementLocated(locator));
    }

    public static boolean waitForStaleness(WebElement element) {
        return defaultWait().until(ExpectedConditions.stalenessOf(element));
    }

    // -------------------------------------------------------------------------
    // Text conditions
    // -------------------------------------------------------------------------

    public static boolean waitForTextPresent(By locator, String text) {
        return defaultWait().until(ExpectedConditions.textToBePresentInElementLocated(locator, text));
    }

    public static boolean waitForTextPresent(WebElement element, String text) {
        return defaultWait().until(ExpectedConditions.textToBePresentInElement(element, text));
    }

    public static boolean waitForValuePresent(By locator, String value) {
        return defaultWait().until(ExpectedConditions.textToBePresentInElementValue(locator, value));
    }

    // -------------------------------------------------------------------------
    // Attribute conditions
    // -------------------------------------------------------------------------

    public static boolean waitForAttributeContains(WebElement element, String attribute, String value) {
        return defaultWait().until(ExpectedConditions.attributeContains(element, attribute, value));
    }

    public static boolean waitForAttributeToBe(By locator, String attribute, String value) {
        return defaultWait().until(ExpectedConditions.attributeToBe(locator, attribute, value));
    }

    // -------------------------------------------------------------------------
    // Page / URL conditions
    // -------------------------------------------------------------------------

    public static boolean waitForUrlContains(String fragment) {
        return defaultWait().until(ExpectedConditions.urlContains(fragment));
    }

    public static boolean waitForUrlMatches(String regex) {
        return defaultWait().until(ExpectedConditions.urlMatches(regex));
    }

    public static boolean waitForTitleContains(String title) {
        return defaultWait().until(ExpectedConditions.titleContains(title));
    }

    // -------------------------------------------------------------------------
    // Frame / Alert / Window conditions
    // -------------------------------------------------------------------------

    public static Alert waitForAlert() {
        return defaultWait().until(ExpectedConditions.alertIsPresent());
    }

    public static WebDriver waitForFrameAndSwitch(By frameLocator) {
        return defaultWait().until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(frameLocator));
    }

    public static WebDriver waitForFrameAndSwitch(int frameIndex) {
        return defaultWait().until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(frameIndex));
    }

    public static boolean waitForNumberOfWindowsToBe(int count) {
        return defaultWait().until(ExpectedConditions.numberOfWindowsToBe(count));
    }

    // -------------------------------------------------------------------------
    // Custom / Fluent conditions
    // -------------------------------------------------------------------------

    /**
     * Waits for an arbitrary custom condition expressed as a lambda.
     *
     * <p>Example: {@code WaitUtils.waitFor(d -> d.findElements(By.cssSelector(".row")).size() > 5)}
     */
    public static <T> T waitFor(Function<WebDriver, T> condition) {
        return waitFor(condition, DEFAULT_TIMEOUT);
    }

    public static <T> T waitFor(Function<WebDriver, T> condition, int seconds) {
        return fluentWait(seconds).until(condition::apply);
    }

    public static <T> T waitFor(ExpectedCondition<T> condition, int seconds) {
        return waitWithTimeout(seconds).until(condition);
    }

    // -------------------------------------------------------------------------
    // Page load / JavaScript ready
    // -------------------------------------------------------------------------

    /** Waits until document.readyState == 'complete'. */
    public static void waitForPageLoad() {
        log.debug("Waiting for page load (document.readyState = 'complete')");
        defaultWait().until(driver ->
            "complete".equals(((JavascriptExecutor) driver)
                .executeScript("return document.readyState")));
    }

    /** Waits until jQuery AJAX requests are complete (if jQuery is on the page). */
    public static void waitForJqueryComplete() {
        log.debug("Waiting for jQuery AJAX to complete");
        defaultWait().until(driver -> {
            try {
                return (Boolean) ((JavascriptExecutor) driver)
                    .executeScript("return typeof jQuery !== 'undefined' && jQuery.active === 0");
            } catch (Exception e) {
                return true; // jQuery not present — treat as complete
            }
        });
    }

    // -------------------------------------------------------------------------
    // Hard sleep (use sparingly — only when no event-based wait exists)
    // -------------------------------------------------------------------------

    /**
     * Thread sleep as a last resort. Prefer event-based waits.
     * This method is intentionally named verbosely to make its use visible in code review.
     */
    public static void hardSleepMillis(long millis) {
        log.warn("Using hard sleep for {}ms — consider replacing with an explicit wait", millis);
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Thread interrupted during sleep", e);
        }
    }
}
