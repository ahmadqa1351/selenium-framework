package com.enterprise.framework.base;

import com.enterprise.framework.driver.DriverManager;
import com.enterprise.framework.reporting.ExtentReportManager;
import com.enterprise.framework.utils.ElementUtils;
import com.enterprise.framework.utils.ScreenshotUtils;
import com.enterprise.framework.utils.WaitUtils;
import com.aventstack.extentreports.Status;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.Select;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Abstract base class for all Page Objects.
 *
 * <p><b>Design decisions:</b>
 * <ul>
 *   <li>Wraps {@link ElementUtils} and {@link WaitUtils} so page classes don't import them
 *       directly — pages stay clean and readable</li>
 *   <li>{@link PageFactory#initElements} initializes {@code @FindBy}-annotated fields</li>
 *   <li>All methods delegate to the ElementUtils/WaitUtils layer — no raw Selenium calls
 *       appear inside page classes</li>
 *   <li>Each action logs to both Log4j2 and Extent Reports — automated test trails</li>
 *   <li>Subclasses override {@link #isLoaded()} for page load validation</li>
 * </ul>
 *
 * <p><b>Page Object contract:</b>
 * <pre>
 *   public class LoginPage extends BasePage {
 *       @FindBy(id = "username") private WebElement usernameField;
 *       @FindBy(id = "password") private WebElement passwordField;
 *
 *       public LoginPage(WebDriver driver) { super(driver); }
 *
 *       public LoginPage enterUsername(String username) {
 *           type(usernameField, username);
 *           return this;
 *       }
 *   }
 * </pre>
 */
public abstract class BasePage {

    protected final WebDriver driver;
    protected final Logger log;
    protected final Actions actions;

    protected BasePage(WebDriver driver) {
        this.driver  = driver;
        this.log     = LogManager.getLogger(getClass());
        this.actions = new Actions(driver);
        PageFactory.initElements(driver, this);
    }

    // =========================================================================
    // Page validation — override in each page class
    // =========================================================================

    /**
     * Returns true if this page is fully loaded and ready for interaction.
     * Override in each page class with a condition appropriate for that page.
     */
    public abstract boolean isLoaded();

    /**
     * Asserts the page is loaded. Call at the end of each page constructor or navigate() method.
     */
    protected void assertLoaded() {
        if (!isLoaded()) {
            throw new IllegalStateException(
                getClass().getSimpleName() + " is not loaded. " +
                "Current URL: " + getPageUrl());
        }
    }

    // =========================================================================
    // Navigation
    // =========================================================================

    public void navigateTo(String url) {
        log.info("Navigating to: {}", url);
        driver.get(url);
        WaitUtils.waitForPageLoad();
    }

    public void navigateBack()    { driver.navigate().back();    WaitUtils.waitForPageLoad(); }
    public void navigateForward() { driver.navigate().forward(); WaitUtils.waitForPageLoad(); }
    public void refreshPage()     { driver.navigate().refresh(); WaitUtils.waitForPageLoad(); }

    public String getPageUrl()    { return driver.getCurrentUrl(); }
    public String getPageTitle()  { return driver.getTitle(); }

    // =========================================================================
    // Click wrappers
    // =========================================================================

    protected void click(By locator) {
        log.debug("Clicking: {}", locator);
        ElementUtils.click(locator);
    }

    protected void click(WebElement element) {
        ElementUtils.click(element);
    }

    protected void jsClick(By locator) {
        log.debug("JS-clicking: {}", locator);
        ElementUtils.jsClick(locator);
    }

    protected void jsClick(WebElement element) {
        ElementUtils.jsClick(element);
    }

    protected void doubleClick(By locator)  { ElementUtils.doubleClick(locator); }
    protected void rightClick(By locator)   { ElementUtils.rightClick(locator); }

    // =========================================================================
    // Type wrappers
    // =========================================================================

    protected void type(By locator, String text) {
        log.debug("Typing '{}' into: {}", text, locator);
        ElementUtils.type(locator, text);
    }

    protected void type(WebElement element, String text) {
        ElementUtils.type(element, text);
    }

    protected void clearAndType(By locator, String text) {
        ElementUtils.clearAndType(locator, text);
    }

    protected void typeSlowly(By locator, String text) {
        // Types character by character — for inputs that react to each keystroke
        WebElement el = WaitUtils.waitForVisible(locator);
        el.clear();
        for (char c : text.toCharArray()) {
            el.sendKeys(String.valueOf(c));
        }
    }

    protected void pressKey(By locator, Keys key) {
        ElementUtils.pressKey(locator, key);
    }

    protected void pressEnter(By locator) {
        ElementUtils.pressKey(locator, Keys.ENTER);
    }

    // =========================================================================
    // Read wrappers
    // =========================================================================

    protected String getText(By locator)             { return ElementUtils.getText(locator); }
    protected String getText(WebElement element)     { return ElementUtils.getText(element); }
    protected String getAttribute(By locator, String attr)     { return ElementUtils.getAttribute(locator, attr); }
    protected String getAttribute(WebElement el, String attr)  { return ElementUtils.getAttribute(el, attr); }
    protected String getValue(By locator)            { return ElementUtils.getValue(locator); }

    // =========================================================================
    // State checks
    // =========================================================================

    protected boolean isDisplayed(By locator)        { return ElementUtils.isDisplayed(locator); }
    protected boolean isDisplayed(WebElement element){ return ElementUtils.isDisplayed(element); }
    protected boolean isEnabled(By locator)          { return ElementUtils.isEnabled(locator); }
    protected boolean isSelected(By locator)         { return ElementUtils.isSelected(locator); }
    protected boolean isPresent(By locator)          { return ElementUtils.isPresent(locator); }

    // =========================================================================
    // Dropdown wrappers
    // =========================================================================

    protected void selectByText(By locator, String text)   { ElementUtils.selectByVisibleText(locator, text); }
    protected void selectByValue(By locator, String value) { ElementUtils.selectByValue(locator, value); }
    protected void selectByIndex(By locator, int index)    { ElementUtils.selectByIndex(locator, index); }
    protected String getSelectedText(By locator)           { return ElementUtils.getSelectedText(locator); }
    protected List<String> getAllOptions(By locator)        { return ElementUtils.getAllOptions(locator); }

    // =========================================================================
    // Wait wrappers
    // =========================================================================

    protected WebElement waitForVisible(By locator)                  { return WaitUtils.waitForVisible(locator); }
    protected WebElement waitForClickable(By locator)                { return WaitUtils.waitForClickable(locator); }
    protected WebElement waitForPresence(By locator)                 { return WaitUtils.waitForPresence(locator); }
    protected boolean waitForInvisible(By locator)                   { return WaitUtils.waitForInvisible(locator); }
    protected boolean waitForTextPresent(By locator, String text)    { return WaitUtils.waitForTextPresent(locator, text); }
    protected boolean waitForUrlContains(String fragment)            { return WaitUtils.waitForUrlContains(fragment); }

    // =========================================================================
    // Alert handling
    // =========================================================================

    protected String acceptAlert() {
        Alert alert = WaitUtils.waitForAlert();
        String text = alert.getText();
        log.debug("Accepting alert: {}", text);
        alert.accept();
        return text;
    }

    protected String dismissAlert() {
        Alert alert = WaitUtils.waitForAlert();
        String text = alert.getText();
        log.debug("Dismissing alert: {}", text);
        alert.dismiss();
        return text;
    }

    protected void typeInAlert(String text) {
        Alert alert = WaitUtils.waitForAlert();
        alert.sendKeys(text);
        alert.accept();
    }

    // =========================================================================
    // Frame handling
    // =========================================================================

    protected void switchToFrame(By frameLocator)  { WaitUtils.waitForFrameAndSwitch(frameLocator); }
    protected void switchToFrame(int frameIndex)   { WaitUtils.waitForFrameAndSwitch(frameIndex); }
    protected void switchToFrame(String frameName) { driver.switchTo().frame(frameName); }
    protected void switchToDefaultContent()        { driver.switchTo().defaultContent(); }
    protected void switchToParentFrame()           { driver.switchTo().parentFrame(); }

    // =========================================================================
    // Window / Tab handling
    // =========================================================================

    protected void switchToNewWindow() {
        String currentHandle = driver.getWindowHandle();
        WaitUtils.waitForNumberOfWindowsToBe(2);
        for (String handle : driver.getWindowHandles()) {
            if (!handle.equals(currentHandle)) {
                driver.switchTo().window(handle);
                log.debug("Switched to new window: {}", driver.getTitle());
                return;
            }
        }
        throw new NoSuchWindowException("No new window found to switch to.");
    }

    protected void closeCurrentWindowAndSwitch(String targetHandle) {
        driver.close();
        driver.switchTo().window(targetHandle);
    }

    protected String getMainWindowHandle() {
        return driver.getWindowHandle();
    }

    protected Set<String> getAllWindowHandles() {
        return driver.getWindowHandles();
    }

    // =========================================================================
    // Scroll helpers
    // =========================================================================

    protected void scrollToElement(By locator)        { ElementUtils.scrollToElement(locator); }
    protected void scrollToElement(WebElement element){ ElementUtils.scrollToElement(element); }
    protected void scrollToTop()                      { ElementUtils.scrollToTop(); }
    protected void scrollToBottom()                   { ElementUtils.scrollToBottom(); }

    // =========================================================================
    // JavaScript
    // =========================================================================

    protected Object executeScript(String script, Object... args) {
        return ((JavascriptExecutor) driver).executeScript(script, args);
    }

    protected void highlightElement(WebElement element) {
        ElementUtils.highlight(element);
    }

    // =========================================================================
    // Screenshot helpers
    // =========================================================================

    protected void takeScreenshot(String stepName) {
        String base64 = ScreenshotUtils.captureAsBase64();
        ExtentReportManager.logWithScreenshot(Status.INFO, stepName);
    }

    // =========================================================================
    // Report step logging
    // =========================================================================

    protected void step(String message) {
        log.info(message);
        ExtentReportManager.logInfo(message);
    }

    protected void stepPass(String message) {
        log.info("✓ {}", message);
        ExtentReportManager.logPass(message);
    }
}
