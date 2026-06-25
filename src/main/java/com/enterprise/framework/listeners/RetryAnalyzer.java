package com.enterprise.framework.listeners;

import com.enterprise.framework.config.EnvironmentConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

/**
 * Configurable retry mechanism for flaky tests.
 *
 * <p><b>Design:</b> Implements TestNG's {@link IRetryAnalyzer}. When a test fails,
 * TestNG calls {@link #retry(ITestResult)}. If this returns {@code true}, the test
 * is re-run up to {@code maxRetryCount} times. The count is thread-local so that
 * parallel threads don't share retry state.
 *
 * <p><b>Retry count configuration:</b> Set {@code retry.count=2} in any properties file.
 * The default is 1 retry. Setting to 0 disables retries entirely.
 *
 * <p><b>Attaching to tests:</b> Two options:
 * <ol>
 *   <li>Per annotation: {@code @Test(retryAnalyzer = RetryAnalyzer.class)}</li>
 *   <li>Globally via {@link TestListener#onTestStart} — recommended for large suites</li>
 * </ol>
 *
 * <p><b>WARNING:</b> Retries mask flakiness. Use this as a safety net while fixing
 * root causes, not as a permanent solution. Monitor your retry rate in reports.
 */
public class RetryAnalyzer implements IRetryAnalyzer {

    private static final Logger log = LogManager.getLogger(RetryAnalyzer.class);
    private static final int MAX_RETRY = EnvironmentConfig.getRetryCount();

    // ThreadLocal counter — each parallel thread has its own retry count per test
    private final ThreadLocal<Integer> retryCount = ThreadLocal.withInitial(() -> 0);

    @Override
    public boolean retry(ITestResult result) {
        int current = retryCount.get();
        if (current < MAX_RETRY) {
            retryCount.set(current + 1);
            log.warn("RETRYING test [{}/{}]: {} — Failure reason: {}",
                current + 1, MAX_RETRY,
                result.getMethod().getMethodName(),
                getFailureMessage(result));
            return true;
        }
        // Reset counter after exhausting retries so the same instance can be reused
        retryCount.set(0);
        log.error("Test FAILED after {} retries: {}", MAX_RETRY, result.getMethod().getMethodName());
        return false;
    }

    private String getFailureMessage(ITestResult result) {
        Throwable t = result.getThrowable();
        return (t != null) ? t.getMessage() : "Unknown reason";
    }
}
