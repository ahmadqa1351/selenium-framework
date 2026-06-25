package com.enterprise.framework.utils;

import com.enterprise.framework.driver.DriverManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.Select;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Resilient element interaction layer — the workhorse of the framework.
 *
 * <p><b>Design philosophy:</b>
 * <ul>
 *   <li>Every method that interacts with the DOM goes through an explicit wait first</li>
 *   <li>StaleElementReferenceException is handled with an automatic re-fetch + retry</li>
 *   <li>JavaScript fallbacks are used when native Selenium interactions fail
 *       (common with custom UI libraries that intercept click events)</li>
 *   <li>All methods are static — no instance needed; driver is pulled from
 *       {@link DriverManager} per-thread</li>
 * </ul>
 */
public final class ElementUtils {

    private static final Logger log = LogManager.getLogger(ElementUtils.class);
    private static final int MAX_RETRY = 3;

    private ElementUtils() { }

    // -------------------------------------------------------------------------
    // Click operations
    // -------------------------------------------------------------------------

    /** Waits for element to be clickable then clicks. */
    public static void click(By locator) {
        log.debug("Clicking element: {}", locator);
        WebElement element = WaitUtils.waitForClickable(locator);
        executeWithRetry(locator, el -> { el.click(); return null; });
    }

    public static void click(WebElement element) {
        log.debug("Clicking WebElement");
        WaitUtils.waitForClickable(element).click();
    }

    /** JavaScript click — use when native click is intercepted or element is off-screen. */
    public static void jsClick(By locator) {
        log.debug("JS-clicking element: {}", locator);
        WebElement element = WaitUtils.waitForPresence(locator);
        jsClick(element);
    }

    public static void jsClick(WebElement element) {
        getJs().executeScript("arguments[0].click();", element);
    }

    /** Actions-based click — use for complex UI components or hover menus. */
    public static void actionsClick(By locator) {
        WebElement element = WaitUtils.waitForClickable(locator);
        new Actions(DriverManager.getDriver()).moveToElement(element).click().perform();
    }

    /** Double-click. */
    public static void doubleClick(By locator) {
        WebElement element = WaitUtils.waitForClickable(locator);
        new Actions(DriverManager.getDriver()).doubleClick(element).perform();
    }

    /** Right-click (context menu). */
    public static void rightClick(By locator) {
        WebElement element = WaitUtils.waitForClickable(locator);
        new Actions(DriverManager.getDriver()).contextClick(element).perform();
    }

    // -------------------------------------------------------------------------
    // Text input
    // -------------------------------------------------------------------------

    /** Clears the field, then types text. */
    public static void type(By locator, String text) {
        log.debug("Typing '{}' into: {}", text, locator);
        WebElement element = WaitUtils.waitForVisible(locator);
        element.clear();
        element.sendKeys(text);
    }

    public static void type(WebElement element, String text) {
        WaitUtils.waitForVisible(element).clear();
        element.sendKeys(text);
    }

    /** Types without clearing (appends to existing value). */
    public static void typeAppend(By locator, String text) {
        WaitUtils.waitForVisible(locator).sendKeys(text);
    }

    /** Clears using keyboard shortcut (more reliable than element.clear() for some frameworks). */
    public static void clearAndType(By locator, String text) {
        WebElement element = WaitUtils.waitForClickable(locator);
        element.sendKeys(Keys.chord(Keys.CONTROL, "a"));
        element.sendKeys(Keys.DELETE);
        element.sendKeys(text);
    }

    /** Types using JavaScript — for fields that block native keyboard events. */
    public static void jsType(By locator, String text) {
        WebElement element = WaitUtils.waitForPresence(locator);
        getJs().executeScript("arguments[0].value = arguments[1];", element, text);
        // Trigger change event so JS frameworks (React/Angular/Vue) detect the change
        getJs().executeScript("arguments[0].dispatchEvent(new Event('input', { bubbles: true }));", element);
        getJs().executeScript("arguments[0].dispatchEvent(new Event('change', { bubbles: true }));", element);
    }

    /** Simulates pressing a key (e.g. ENTER, TAB, ESCAPE). */
    public static void pressKey(By locator, Keys key) {
        WaitUtils.waitForVisible(locator).sendKeys(key);
    }

    // -------------------------------------------------------------------------
    // Element state checks
    // -------------------------------------------------------------------------

    public static boolean isDisplayed(By locator) {
        try {
            return DriverManager.getDriver().findElement(locator).isDisplayed();
        } catch (NoSuchElementException | StaleElementReferenceException e) {
            return false;
        }
    }

    public static boolean isDisplayed(WebElement element) {
        try {
            return element.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isEnabled(By locator) {
        try {
            return DriverManager.getDriver().findElement(locator).isEnabled();
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isSelected(By locator) {
        try {
            return DriverManager.getDriver().findElement(locator).isSelected();
        } catch (Exception e) {
            return false;
        }
    }

    /** Returns true if element is present in DOM (not necessarily visible). */
    public static boolean isPresent(By locator) {
        return !DriverManager.getDriver().findElements(locator).isEmpty();
    }

    // -------------------------------------------------------------------------
    // Text retrieval
    // -------------------------------------------------------------------------

    public static String getText(By locator) {
        return WaitUtils.waitForVisible(locator).getText().trim();
    }

    public static String getText(WebElement element) {
        return WaitUtils.waitForVisible(element).getText().trim();
    }

    public static String getAttribute(By locator, String attribute) {
        return WaitUtils.waitForPresence(locator).getAttribute(attribute);
    }

    public static String getAttribute(WebElement element, String attribute) {
        return element.getAttribute(attribute);
    }

    public static String getValue(By locator) {
        return getAttribute(locator, "value");
    }

    public static List<String> getTextFromAll(By locator) {
        return WaitUtils.waitForAllVisible(locator).stream()
            .map(el -> el.getText().trim())
            .collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // Dropdown handling
    // -------------------------------------------------------------------------

    public static void selectByVisibleText(By locator, String text) {
        log.debug("Selecting '{}' by visible text from: {}", text, locator);
        new Select(WaitUtils.waitForVisible(locator)).selectByVisibleText(text);
    }

    public static void selectByValue(By locator, String value) {
        log.debug("Selecting '{}' by value from: {}", value, locator);
        new Select(WaitUtils.waitForVisible(locator)).selectByValue(value);
    }

    public static void selectByIndex(By locator, int index) {
        new Select(WaitUtils.waitForVisible(locator)).selectByIndex(index);
    }

    public static String getSelectedText(By locator) {
        return new Select(WaitUtils.waitForVisible(locator)).getFirstSelectedOption().getText();
    }

    public static List<String> getAllOptions(By locator) {
        return new Select(WaitUtils.waitForVisible(locator)).getOptions().stream()
            .map(WebElement::getText)
            .collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // Checkbox / Radio
    // -------------------------------------------------------------------------

    /** Ensures the checkbox is checked. Does nothing if already checked. */
    public static void check(By locator) {
        WebElement el = WaitUtils.waitForClickable(locator);
        if (!el.isSelected()) {
            el.click();
            log.debug("Checked: {}", locator);
        }
    }

    /** Ensures the checkbox is unchecked. Does nothing if already unchecked. */
    public static void uncheck(By locator) {
        WebElement el = WaitUtils.waitForClickable(locator);
        if (el.isSelected()) {
            el.click();
            log.debug("Unchecked: {}", locator);
        }
    }

    // -------------------------------------------------------------------------
    // Scroll utilities
    // -------------------------------------------------------------------------

    public static void scrollToElement(WebElement element) {
        getJs().executeScript("arguments[0].scrollIntoView({block: 'center', inline: 'center'});", element);
    }

    public static void scrollToElement(By locator) {
        scrollToElement(DriverManager.getDriver().findElement(locator));
    }

    public static void scrollToTop() {
        getJs().executeScript("window.scrollTo(0, 0);");
    }

    public static void scrollToBottom() {
        getJs().executeScript("window.scrollTo(0, document.body.scrollHeight);");
    }

    public static void scrollBy(int x, int y) {
        getJs().executeScript("window.scrollBy(arguments[0], arguments[1]);", x, y);
    }

    // -------------------------------------------------------------------------
    // Hover / Drag & Drop
    // -------------------------------------------------------------------------

    public static void hover(By locator) {
        WebElement element = WaitUtils.waitForVisible(locator);
        new Actions(DriverManager.getDriver()).moveToElement(element).perform();
    }

    public static void dragAndDrop(By sourceLocator, By targetLocator) {
        WebElement source = WaitUtils.waitForVisible(sourceLocator);
        WebElement target = WaitUtils.waitForVisible(targetLocator);
        new Actions(DriverManager.getDriver()).dragAndDrop(source, target).perform();
    }

    // -------------------------------------------------------------------------
    // Multiple elements
    // -------------------------------------------------------------------------

    public static List<WebElement> findAll(By locator) {
        return DriverManager.getDriver().findElements(locator);
    }

    public static int countElements(By locator) {
        return DriverManager.getDriver().findElements(locator).size();
    }

    // -------------------------------------------------------------------------
    // Highlighting (for debugging)
    // -------------------------------------------------------------------------

    /** Highlights an element with a red border — useful during local debugging. */
    public static void highlight(WebElement element) {
        getJs().executeScript(
            "arguments[0].style.border='3px solid red'; " +
            "arguments[0].style.backgroundColor='#FFFF99';", element);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private static JavascriptExecutor getJs() {
        return (JavascriptExecutor) DriverManager.getDriver();
    }

    /**
     * Executes an action on an element with automatic retry on {@link StaleElementReferenceException}.
     * Stale elements are common in single-page apps where the DOM updates after partial renders.
     */
    private static <T> T executeWithRetry(By locator, java.util.function.Function<WebElement, T> action) {
        int attempts = 0;
        while (attempts < MAX_RETRY) {
            try {
                WebElement element = DriverManager.getDriver().findElement(locator);
                return action.apply(element);
            } catch (StaleElementReferenceException e) {
                attempts++;
                log.warn("StaleElementReferenceException on attempt {}/{} for locator: {}", attempts, MAX_RETRY, locator);
                if (attempts == MAX_RETRY) {
                    throw new RuntimeException("Element remained stale after " + MAX_RETRY + " retries: " + locator, e);
                }
                WaitUtils.hardSleepMillis(200);
            }
        }
        throw new IllegalStateException("Should not reach here");
    }
}
