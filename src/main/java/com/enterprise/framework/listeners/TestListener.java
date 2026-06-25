package com.enterprise.framework.listeners;

import com.aventstack.extentreports.Status;
import com.enterprise.framework.driver.DriverManager;
import com.enterprise.framework.reporting.ExtentReportManager;
import com.enterprise.framework.utils.ScreenshotUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.*;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Central TestNG event listener — the glue between TestNG, Extent Reports, and logging.
 *
 * <p><b>Responsibilities:</b>
 * <ul>
 *   <li>Initialize/flush Extent Reports at suite level</li>
 *   <li>Create per-test report nodes with metadata</li>
 *   <li>Capture screenshots on failure and embed them in the report</li>
 *   <li>Log test pass/fail/skip with full context</li>
 *   <li>Attach {@link RetryAnalyzer} globally to all tests</li>
 *   <li>Emit structured Log4j2 entries for each test lifecycle event</li>
 * </ul>
 *
 * <p><b>Registration:</b> Added to each testng.xml suite:
 * <pre>
 *   &lt;listeners&gt;
 *     &lt;listener class-name="com.enterprise.framework.listeners.TestListener"/&gt;
 *   &lt;/listeners&gt;
 * </pre>
 */
public class TestListener implements ISuiteListener, ITestListener, IInvokedMethodListener {

    private static final Logger log = LogManager.getLogger(TestListener.class);

    // =========================================================================
    // ISuiteListener — suite-level lifecycle
    // =========================================================================

    @Override
    public void onStart(ISuite suite) {
        log.info("╔══════════════════════════════════════════════════════╗");
        log.info("║  SUITE STARTED: {}",  suite.getName());
        log.info("╚══════════════════════════════════════════════════════╝");
        ExtentReportManager.initReports();
    }

    @Override
    public void onFinish(ISuite suite) {
        ExtentReportManager.flushReports();
        log.info("╔══════════════════════════════════════════════════════╗");
        log.info("║  SUITE FINISHED: {}", suite.getName());
        printSuiteSummary(suite);
        log.info("╚══════════════════════════════════════════════════════╝");
    }

    // =========================================================================
    // ITestListener — individual test lifecycle
    // =========================================================================

    @Override
    public void onTestStart(ITestResult result) {
        String testName = getFullTestName(result);
        String[] groups = result.getMethod().getGroups();

        log.info("▶  STARTED  | {}", testName);

        // Create the Extent Report node
        ExtentReportManager.createTest(testName, groups);
        ExtentReportManager.logInfo("Test started on browser: " + System.getProperty("browser", "chrome"));

        // Globally attach RetryAnalyzer to every test method
        result.getMethod().setRetryAnalyzer(new RetryAnalyzer());
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        String testName = getFullTestName(result);
        long duration = result.getEndMillis() - result.getStartMillis();

        log.info("✅  PASSED   | {} | {}ms", testName, duration);
        ExtentReportManager.logPass("Test passed in " + duration + "ms");
    }

    @Override
    public void onTestFailure(ITestResult result) {
        String testName = getFullTestName(result);
        Throwable throwable = result.getThrowable();
        long duration = result.getEndMillis() - result.getStartMillis();

        log.error("❌  FAILED   | {} | {}ms", testName, duration);
        log.error("   Reason: {}", throwable != null ? throwable.getMessage() : "Unknown");

        // Capture screenshot and embed in report
        String base64Screenshot = ScreenshotUtils.captureAsBase64();
        if (!base64Screenshot.isEmpty()) {
            ExtentReportManager.logFail("Test FAILED: " + (throwable != null ? throwable.getMessage() : ""), base64Screenshot);
        } else {
            ExtentReportManager.logFail("Test FAILED: " + (throwable != null ? throwable.getMessage() : ""));
        }

        // Also log the stack trace in the report
        if (throwable != null) {
            ExtentReportManager.logFail(throwable);
        }

        // Save screenshot to disk (for CI artifact collection)
        String screenshotPath = ScreenshotUtils.captureAndSave(testName.replaceAll("\\s", "_"));
        if (!screenshotPath.isEmpty()) {
            log.info("   Screenshot saved: {}", screenshotPath);
        }
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        String testName = getFullTestName(result);
        log.warn("⏭  SKIPPED  | {}", testName);

        Throwable cause = result.getThrowable();
        String reason = (cause != null) ? cause.getMessage() : "No reason provided";
        ExtentReportManager.logSkip("Test skipped: " + reason);
    }

    @Override
    public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
        log.warn("⚠  WITHIN SUCCESS% | {}", getFullTestName(result));
    }

    // =========================================================================
    // IInvokedMethodListener — configuration method lifecycle (@Before/@After)
    // =========================================================================

    @Override
    public void afterInvocation(IInvokedMethod method, ITestResult testResult) {
        // Clean up ExtentTest ThreadLocal after each test method invocation
        if (method.isTestMethod()) {
            ExtentReportManager.removeTest();
        }
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    /**
     * Builds a readable test name including class name, method name, and parameters.
     * Example: {@code LoginTest.loginWithValidCredentials[admin@test.com, password123]}
     */
    private String getFullTestName(ITestResult result) {
        String className  = result.getTestClass().getRealClass().getSimpleName();
        String methodName = result.getMethod().getMethodName();

        Object[] params = result.getParameters();
        if (params != null && params.length > 0) {
            String paramStr = Arrays.stream(params)
                .map(p -> p != null ? p.toString() : "null")
                .collect(Collectors.joining(", "));
            return className + "." + methodName + "[" + paramStr + "]";
        }
        return className + "." + methodName;
    }

    private void printSuiteSummary(ISuite suite) {
        var results = suite.getResults();
        int passed = 0, failed = 0, skipped = 0;
        for (var entry : results.entrySet()) {
            var context = entry.getValue().getTestContext();
            passed  += context.getPassedTests().size();
            failed  += context.getFailedTests().size();
            skipped += context.getSkippedTests().size();
        }
        log.info("  Results → Passed: {} | Failed: {} | Skipped: {}", passed, failed, skipped);
    }
}
