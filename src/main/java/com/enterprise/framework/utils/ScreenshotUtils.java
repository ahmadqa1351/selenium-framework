package com.enterprise.framework.utils;

import com.enterprise.framework.config.EnvironmentConfig;
import com.enterprise.framework.driver.DriverManager;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

/**
 * Thread-safe screenshot capture utility.
 *
 * <p><b>Features:</b>
 * <ul>
 *   <li>Full-page screenshot to file (with auto-created directory)</li>
 *   <li>Element-scoped screenshot</li>
 *   <li>Base64-encoded screenshot for inline embedding in Extent Reports</li>
 *   <li>Timestamped, sanitized filenames that survive CI artifact collection</li>
 * </ul>
 *
 * <p><b>Output path:</b> {@code {report.dir}/screenshots/{testName}_{timestamp}.png}
 */
public final class ScreenshotUtils {

    private static final Logger log = LogManager.getLogger(ScreenshotUtils.class);
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");

    private ScreenshotUtils() { }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Captures a full-page screenshot and saves it to the configured reports directory.
     *
     * @param testName test name used to build a meaningful file name
     * @return absolute path of the saved screenshot, or empty string on failure
     */
    public static String captureAndSave(String testName) {
        if (!DriverManager.isDriverInitialized()) {
            log.warn("Screenshot skipped — no WebDriver initialized for this thread.");
            return "";
        }
        try {
            File src = ((TakesScreenshot) DriverManager.getDriver()).getScreenshotAs(OutputType.FILE);
            String fileName = buildFileName(testName);
            Path dest = ensureScreenshotDir().resolve(fileName);
            FileUtils.copyFile(src, dest.toFile());
            log.info("Screenshot saved: {}", dest.toAbsolutePath());
            return dest.toAbsolutePath().toString();
        } catch (IOException e) {
            log.error("Failed to save screenshot for test '{}': {}", testName, e.getMessage(), e);
            return "";
        }
    }

    /**
     * Captures a screenshot of a specific element.
     *
     * @param element  the WebElement to screenshot
     * @param testName prefix for the file name
     * @return absolute path of the saved screenshot, or empty string on failure
     */
    public static String captureElement(WebElement element, String testName) {
        try {
            File src = element.getScreenshotAs(OutputType.FILE);
            String fileName = buildFileName(testName + "_element");
            Path dest = ensureScreenshotDir().resolve(fileName);
            FileUtils.copyFile(src, dest.toFile());
            log.info("Element screenshot saved: {}", dest.toAbsolutePath());
            return dest.toAbsolutePath().toString();
        } catch (IOException e) {
            log.error("Failed to save element screenshot: {}", e.getMessage(), e);
            return "";
        }
    }

    /**
     * Returns a Base64-encoded screenshot string suitable for embedding directly
     * in Extent Reports HTML without storing a file on disk.
     *
     * @return Base64 string, or empty string on failure
     */
    public static String captureAsBase64() {
        if (!DriverManager.isDriverInitialized()) {
            log.warn("Base64 screenshot skipped — no WebDriver initialized.");
            return "";
        }
        try {
            return ((TakesScreenshot) DriverManager.getDriver()).getScreenshotAs(OutputType.BASE64);
        } catch (Exception e) {
            log.error("Failed to capture Base64 screenshot: {}", e.getMessage(), e);
            return "";
        }
    }

    /**
     * Returns raw screenshot bytes — useful for attaching to third-party reporting tools
     * (e.g. Allure, Azure DevOps, JIRA).
     */
    public static byte[] captureAsBytes() {
        if (!DriverManager.isDriverInitialized()) {
            log.warn("Byte screenshot skipped — no WebDriver initialized.");
            return new byte[0];
        }
        try {
            return ((TakesScreenshot) DriverManager.getDriver()).getScreenshotAs(OutputType.BYTES);
        } catch (Exception e) {
            log.error("Failed to capture screenshot bytes: {}", e.getMessage(), e);
            return new byte[0];
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private static String buildFileName(String testName) {
        // Sanitize: replace chars that are invalid in file names on Windows & Linux
        String safe = testName.replaceAll("[^a-zA-Z0-9_\\-]", "_");
        return safe + "_" + LocalDateTime.now().format(TS) + ".png";
    }

    private static Path ensureScreenshotDir() throws IOException {
        Path dir = Paths.get(EnvironmentConfig.getReportDir(), "screenshots");
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }
        return dir;
    }
}
