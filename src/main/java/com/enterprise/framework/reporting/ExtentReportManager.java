package com.enterprise.framework.reporting;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.MediaEntityBuilder;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import com.enterprise.framework.config.ConfigReader;
import com.enterprise.framework.config.EnvironmentConfig;
import com.enterprise.framework.utils.DateUtils;
import com.enterprise.framework.utils.ScreenshotUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Thread-safe singleton manager for Extent Reports.
 *
 * <p><b>Architecture:</b>
 * <ul>
 *   <li>{@code ExtentReports} (singleton) — the shared report document, flushed once after all tests</li>
 *   <li>{@code ExtentTest} (ThreadLocal) — one node per test thread; enables parallel-safe logging</li>
 * </ul>
 *
 * <p><b>Report output:</b> {@code {report.dir}/extent-report_{timestamp}.html}
 *
 * <p><b>Usage flow:</b>
 * <pre>
 *   // In @BeforeSuite
 *   ExtentReportManager.initReports();
 *
 *   // In @BeforeMethod
 *   ExtentReportManager.createTest("TC-001 - Login with valid credentials", "Smoke");
 *
 *   // In tests
 *   ExtentReportManager.logInfo("Navigating to login page");
 *   ExtentReportManager.logPass("User logged in successfully");
 *
 *   // In @AfterMethod (via TestListener)
 *   ExtentReportManager.logFail("Element not found", screenshotBase64);
 *
 *   // In @AfterSuite
 *   ExtentReportManager.flushReports();
 * </pre>
 */
public final class ExtentReportManager {

    private static final Logger log = LogManager.getLogger(ExtentReportManager.class);

    private static volatile ExtentReports extentReports;
    private static final ThreadLocal<ExtentTest> testHolder = new ThreadLocal<>();

    // -------------------------------------------------------------------------
    // Initialization
    // -------------------------------------------------------------------------

    /**
     * Creates the ExtentReports instance and configures the Spark (HTML) reporter.
     * Must be called exactly once before any tests run (in @BeforeSuite).
     */
    public static synchronized void initReports() {
        if (extentReports != null) return; // already initialized

        String reportDir = EnvironmentConfig.getReportDir();
        ensureDir(reportDir);

        String reportPath = reportDir + "/extent-report_" + DateUtils.reportTimestamp() + ".html";
        log.info("Extent Report will be written to: {}", reportPath);

        ExtentSparkReporter sparkReporter = new ExtentSparkReporter(reportPath);
        configureSparkReporter(sparkReporter);

        extentReports = new ExtentReports();
        extentReports.attachReporter(sparkReporter);
        extentReports.setSystemInfo("Environment",  ConfigReader.getEnvironment().toUpperCase());
        extentReports.setSystemInfo("Browser",      EnvironmentConfig.getBrowser());
        extentReports.setSystemInfo("Base URL",     EnvironmentConfig.getBaseUrl());
        extentReports.setSystemInfo("OS",           System.getProperty("os.name"));
        extentReports.setSystemInfo("Java Version", System.getProperty("java.version"));
        extentReports.setSystemInfo("Executed By",  System.getProperty("user.name"));
    }

    // -------------------------------------------------------------------------
    // Test node management
    // -------------------------------------------------------------------------

    /**
     * Creates a new ExtentTest node and binds it to the current thread.
     *
     * @param testName  descriptive test name (visible in the report tree)
     * @param groups    comma-separated test groups (e.g. "Smoke", "Regression")
     */
    public static void createTest(String testName, String... groups) {
        ExtentTest test = extentReports.createTest(testName);
        if (groups != null) {
            for (String group : groups) {
                if (group != null && !group.isBlank()) {
                    test.assignCategory(group);
                }
            }
        }
        testHolder.set(test);
        log.debug("ExtentTest created for: {}", testName);
    }

    /**
     * Returns the ExtentTest for the current thread.
     *
     * @throws IllegalStateException if called before createTest()
     */
    public static ExtentTest getTest() {
        ExtentTest test = testHolder.get();
        if (test == null) {
            throw new IllegalStateException(
                "ExtentTest is not initialized for thread [" + Thread.currentThread().getName() +
                "]. Call createTest() first.");
        }
        return test;
    }

    // -------------------------------------------------------------------------
    // Logging methods
    // -------------------------------------------------------------------------

    public static void logInfo(String message) {
        getTest().log(Status.INFO, message);
    }

    public static void logPass(String message) {
        getTest().log(Status.PASS, message);
    }

    public static void logWarning(String message) {
        getTest().log(Status.WARNING, message);
    }

    /**
     * Logs a failure with an inline Base64 screenshot embedded in the report.
     *
     * @param message        failure description
     * @param base64Screenshot Base64 string from {@link ScreenshotUtils#captureAsBase64()}
     */
    public static void logFail(String message, String base64Screenshot) {
        ExtentTest test = getTest();
        if (base64Screenshot != null && !base64Screenshot.isBlank()) {
            test.fail(message,
                MediaEntityBuilder.createScreenCaptureFromBase64String(base64Screenshot).build());
        } else {
            test.fail(message);
        }
    }

    public static void logFail(String message) {
        getTest().fail(message);
    }

    public static void logFail(Throwable throwable) {
        getTest().fail(throwable);
    }

    public static void logSkip(String message) {
        getTest().skip(message);
    }

    /** Logs a step with an inline screenshot — useful for visual walkthroughs. */
    public static void logWithScreenshot(Status status, String message) {
        String base64 = ScreenshotUtils.captureAsBase64();
        if (!base64.isEmpty()) {
            getTest().log(status, message,
                MediaEntityBuilder.createScreenCaptureFromBase64String(base64).build());
        } else {
            getTest().log(status, message);
        }
    }

    // -------------------------------------------------------------------------
    // Finalization
    // -------------------------------------------------------------------------

    /**
     * Flushes all test results to the HTML report file.
     * Must be called exactly once after all tests complete (in @AfterSuite).
     */
    public static synchronized void flushReports() {
        if (extentReports != null) {
            extentReports.flush();
            log.info("Extent Reports flushed successfully.");
        }
    }

    /** Removes the thread-local test node after the test method completes. */
    public static void removeTest() {
        testHolder.remove();
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private static void configureSparkReporter(ExtentSparkReporter reporter) {
        reporter.config().setTheme(Theme.DARK);
        reporter.config().setDocumentTitle("Automation Execution Report");
        reporter.config().setReportName("Enterprise QA Framework — Test Results");
        reporter.config().setTimeStampFormat("EEEE, MMMM dd, yyyy, hh:mm a");
        reporter.config().setEncoding("utf-8");
    }

    private static void ensureDir(String path) {
        try {
            Files.createDirectories(Paths.get(path));
        } catch (IOException e) {
            throw new RuntimeException("Could not create report directory: " + path, e);
        }
    }
}
