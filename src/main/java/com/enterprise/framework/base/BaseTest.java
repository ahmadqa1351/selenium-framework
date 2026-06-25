package com.enterprise.framework.base;

import com.enterprise.framework.config.EnvironmentConfig;
import com.enterprise.framework.driver.BrowserType;
import com.enterprise.framework.driver.DriverFactory;
import com.enterprise.framework.driver.DriverManager;
import com.enterprise.framework.reporting.ExtentReportManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.ITestContext;
import org.testng.annotations.*;

/**
 * Abstract base class for all test classes.
 *
 * <p><b>Responsibilities:</b>
 * <ul>
 *   <li>Create and destroy WebDriver instances per test method (thread-safe)</li>
 *   <li>Navigate to the base URL before each test</li>
 *   <li>Provide typed page object factory method {@link #getPage(Class)}</li>
 *   <li>Log test start/end boundaries to Log4j2</li>
 * </ul>
 *
 * <p><b>Lifecycle (executed per thread):</b>
 * <pre>
 *   @BeforeClass  → logClassSetup
 *   @BeforeMethod → initDriver → navigate to base URL
 *   @Test         → (test body runs here)
 *   @AfterMethod  → quitDriver
 *   @AfterClass   → logClassTeardown
 * </pre>
 *
 * <p><b>Extension:</b> Override {@link #beforeTest()} and {@link #afterTest()} in subclasses
 * for class-specific setup/teardown without breaking the driver lifecycle.
 *
 * <p><b>Parallel execution:</b> Using {@code @BeforeMethod}/{@code @AfterMethod} (not
 * {@code @BeforeClass}) guarantees each parallel thread has its own driver. Never use
 * static or shared WebDriver fields in test classes.
 */
@Listeners(com.enterprise.framework.listeners.TestListener.class)
public abstract class BaseTest {

    protected final Logger log = LogManager.getLogger(getClass());

    // =========================================================================
    // Suite-level hooks (executed once per suite — not per thread)
    // =========================================================================

    @BeforeSuite(alwaysRun = true)
    public void suiteSetup(ITestContext context) {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("  ENVIRONMENT : {}", EnvironmentConfig.getEnvironment().toUpperCase());
        log.info("  BASE URL    : {}", EnvironmentConfig.getBaseUrl());
        log.info("  BROWSER     : {}", EnvironmentConfig.getBrowser());
        log.info("  GRID        : {}", EnvironmentConfig.isGridExecution() ? EnvironmentConfig.getGridUrl() : "LOCAL");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    @AfterSuite(alwaysRun = true)
    public void suiteTeardown() {
        log.info("Suite teardown complete.");
    }

    // =========================================================================
    // Class-level hooks
    // =========================================================================

    @BeforeClass(alwaysRun = true)
    public void classSetup() {
        log.debug("Class setup: {}", getClass().getSimpleName());
    }

    @AfterClass(alwaysRun = true)
    public void classTeardown() {
        log.debug("Class teardown: {}", getClass().getSimpleName());
    }

    // =========================================================================
    // Method-level hooks — thread-safe driver lifecycle
    // =========================================================================

    /**
     * Creates a fresh WebDriver and navigates to the base URL before each test.
     * The {@code @Parameters} annotation reads from testng.xml, falling back to the
     * system property {@code -Dbrowser} and then to the properties file value.
     */
    @BeforeMethod(alwaysRun = true)
    @Parameters({"browser"})
    public void setUp(@Optional String browserParam) {
        // Resolution priority: @Parameters (testng.xml) → -Dbrowser → config.properties
        String browserName = (browserParam != null && !browserParam.isBlank())
            ? browserParam
            : EnvironmentConfig.getBrowser();

        BrowserType browserType = BrowserType.fromString(browserName);
        log.info("Initializing {} driver for test on thread [{}]",
            browserType, Thread.currentThread().getName());

        WebDriver driver = DriverFactory.createDriver(browserType);
        DriverManager.setDriver(driver);

        // Navigate to base URL
        String baseUrl = EnvironmentConfig.getBaseUrl();
        log.info("Navigating to base URL: {}", baseUrl);
        driver.get(baseUrl);

        // Hook for subclass-specific setup (API auth, DB seed data, etc.)
        beforeTest();
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        // Hook for subclass-specific cleanup first
        afterTest();

        // Always quit the driver, even if the test threw
        DriverManager.quitDriver();
        log.debug("WebDriver released for thread [{}]", Thread.currentThread().getName());
    }

    // =========================================================================
    // Extension hooks — override in subclasses
    // =========================================================================

    /**
     * Called after driver initialization and URL navigation, before the test method.
     * Override for test-class-specific preconditions (e.g., API login to get a session token).
     */
    protected void beforeTest() { }

    /**
     * Called after the test method completes, before driver quit.
     * Override for test-class-specific cleanup (e.g., delete test data via API).
     */
    protected void afterTest() { }

    // =========================================================================
    // Utility accessors for subclasses
    // =========================================================================

    /** Returns the WebDriver for the current thread. */
    protected WebDriver getDriver() {
        return DriverManager.getDriver();
    }

    /**
     * Factory method to instantiate Page Objects using the current thread's driver.
     *
     * <p><b>Usage:</b> {@code LoginPage loginPage = getPage(LoginPage.class);}
     *
     * @param pageClass Page Object class (must have a constructor accepting WebDriver)
     * @return initialized Page Object instance
     */
    protected <T extends BasePage> T getPage(Class<T> pageClass) {
        try {
            T page = pageClass
                .getDeclaredConstructor(org.openqa.selenium.WebDriver.class)
                .newInstance(getDriver());
            log.debug("Page Object created: {}", pageClass.getSimpleName());
            return page;
        } catch (Exception e) {
            throw new RuntimeException(
                "Failed to instantiate Page Object: " + pageClass.getName() +
                ". Ensure it has a public constructor accepting WebDriver.", e);
        }
    }

    /** Logs a step to both Log4j2 and Extent Reports. */
    protected void step(String message) {
        log.info("  ▸ {}", message);
        ExtentReportManager.logInfo(message);
    }
}
