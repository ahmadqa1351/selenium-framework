package com.enterprise.framework.driver;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;

/**
 * Thread-safe WebDriver lifecycle manager.
 *
 * <p><b>Design:</b> Uses {@link ThreadLocal} so each parallel thread owns an independent
 * WebDriver instance. This is the cornerstone of parallel-safe execution — no shared
 * mutable state between threads. Every thread that calls {@code setDriver()} gets its
 * own slot; {@code quitDriver()} cleans only that thread's driver.
 *
 * <p><b>Usage contract:</b>
 * <ol>
 *   <li>{@code DriverManager.setDriver(driver)} — called once per test thread in @BeforeMethod</li>
 *   <li>{@code DriverManager.getDriver()} — called anywhere during the test</li>
 *   <li>{@code DriverManager.quitDriver()} — called in @AfterMethod to release resources</li>
 * </ol>
 *
 * <p><b>Memory leak prevention:</b> {@code remove()} is explicitly called after quit to prevent
 * the ThreadLocal from holding a reference to a dead driver object when threads are pooled
 * (common in TestNG parallel mode and CI agents that reuse JVM thread pools).
 */
public final class DriverManager {

    private static final Logger log = LogManager.getLogger(DriverManager.class);

    /** One WebDriver instance per thread. Never share this across threads. */
    private static final ThreadLocal<WebDriver> driverHolder = new ThreadLocal<>();

    private DriverManager() {
        // Utility class — prevent instantiation
    }

    /**
     * Returns the WebDriver for the current thread.
     *
     * @throws IllegalStateException if the driver has not been initialized for this thread
     */
    public static WebDriver getDriver() {
        WebDriver driver = driverHolder.get();
        if (driver == null) {
            throw new IllegalStateException(
                "WebDriver is not initialized for thread [" + Thread.currentThread().getName() +
                "]. Ensure setDriver() is called before getDriver() (typically in @BeforeMethod)."
            );
        }
        return driver;
    }

    /**
     * Binds a WebDriver to the current thread.
     *
     * @param driver non-null WebDriver instance created by {@link DriverFactory}
     * @throws IllegalArgumentException if driver is null
     */
    public static void setDriver(WebDriver driver) {
        if (driver == null) {
            throw new IllegalArgumentException("Cannot register a null WebDriver.");
        }
        log.debug("Binding WebDriver to thread [{}]", Thread.currentThread().getName());
        driverHolder.set(driver);
    }

    /**
     * Quits the WebDriver for the current thread and removes it from the ThreadLocal.
     * Safe to call even if no driver is initialized (no-op).
     */
    public static void quitDriver() {
        WebDriver driver = driverHolder.get();
        if (driver != null) {
            String thread = Thread.currentThread().getName();
            log.info("Quitting WebDriver on thread [{}]", thread);
            try {
                driver.quit();
                log.debug("WebDriver quit successfully on thread [{}]", thread);
            } catch (Exception e) {
                log.warn("Exception while quitting WebDriver on thread [{}]: {}", thread, e.getMessage());
            } finally {
                // CRITICAL: remove() prevents memory leaks in thread pools
                driverHolder.remove();
                log.debug("WebDriver removed from ThreadLocal on thread [{}]", thread);
            }
        }
    }

    /**
     * Checks whether a WebDriver has been initialized for the current thread.
     * Useful for conditional teardown and defensive coding.
     */
    public static boolean isDriverInitialized() {
        return driverHolder.get() != null;
    }
}
