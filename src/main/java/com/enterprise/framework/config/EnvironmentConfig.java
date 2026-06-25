package com.enterprise.framework.config;

/**
 * Strongly-typed wrapper around environment-specific configuration.
 *
 * <p>Provides a clean API for accessing frequently-used config values without
 * scattering raw string keys throughout the test code. All keys are centralized
 * here as constants to prevent typo-driven bugs.
 *
 * <p><b>Adding a new config value:</b>
 * <ol>
 *   <li>Define a constant key below</li>
 *   <li>Add a typed getter method</li>
 *   <li>Add the key/value to each environment properties file</li>
 * </ol>
 */
public final class EnvironmentConfig {

    // -------------------------------------------------------------------------
    // Config key constants — use these instead of magic strings throughout tests
    // -------------------------------------------------------------------------
    private static final String KEY_BASE_URL           = "base.url";
    private static final String KEY_API_BASE_URL       = "api.base.url";
    private static final String KEY_ADMIN_USER         = "admin.username";
    private static final String KEY_ADMIN_PASS         = "admin.password";
    private static final String KEY_TEST_USER          = "test.username";
    private static final String KEY_TEST_PASS          = "test.password";
    private static final String KEY_DB_URL             = "db.url";
    private static final String KEY_DB_USER            = "db.username";
    private static final String KEY_DB_PASS            = "db.password";
    private static final String KEY_DB_DRIVER          = "db.driver";
    private static final String KEY_API_KEY            = "api.key";
    private static final String KEY_BROWSER            = "browser";
    private static final String KEY_GRID_URL           = "grid.url";
    private static final String KEY_IMPLICIT_WAIT      = "implicit.wait.seconds";
    private static final String KEY_EXPLICIT_WAIT      = "explicit.wait.seconds";
    private static final String KEY_RETRY_COUNT        = "retry.count";
    private static final String KEY_SCREENSHOT_ON_PASS = "screenshot.on.pass";
    private static final String KEY_REPORT_DIR         = "report.dir";

    private EnvironmentConfig() { }

    // -------------------------------------------------------------------------
    // Application URLs
    // -------------------------------------------------------------------------

    public static String getBaseUrl() {
        return ConfigReader.getRequired(KEY_BASE_URL);
    }

    public static String getApiBaseUrl() {
        return ConfigReader.get(KEY_API_BASE_URL, getBaseUrl() + "/api");
    }

    // -------------------------------------------------------------------------
    // Credentials
    // -------------------------------------------------------------------------

    public static String getAdminUsername() {
        return ConfigReader.getRequired(KEY_ADMIN_USER);
    }

    public static String getAdminPassword() {
        return ConfigReader.getRequired(KEY_ADMIN_PASS);
    }

    public static String getTestUsername() {
        return ConfigReader.get(KEY_TEST_USER, "testuser");
    }

    public static String getTestPassword() {
        return ConfigReader.get(KEY_TEST_PASS, "testpass");
    }

    public static String getApiKey() {
        return ConfigReader.get(KEY_API_KEY, "");
    }

    // -------------------------------------------------------------------------
    // Database
    // -------------------------------------------------------------------------

    public static String getDbUrl() {
        return ConfigReader.get(KEY_DB_URL, "");
    }

    public static String getDbUsername() {
        return ConfigReader.get(KEY_DB_USER, "");
    }

    public static String getDbPassword() {
        return ConfigReader.get(KEY_DB_PASS, "");
    }

    public static String getDbDriver() {
        return ConfigReader.get(KEY_DB_DRIVER, "com.mysql.cj.jdbc.Driver");
    }

    // -------------------------------------------------------------------------
    // WebDriver / Execution
    // -------------------------------------------------------------------------

    public static String getBrowser() {
        return ConfigReader.get(KEY_BROWSER, "chrome");
    }

    public static String getGridUrl() {
        return ConfigReader.get(KEY_GRID_URL, "");
    }

    public static int getImplicitWaitSeconds() {
        return ConfigReader.getInt(KEY_IMPLICIT_WAIT, 0);
    }

    public static int getExplicitWaitSeconds() {
        return ConfigReader.getInt(KEY_EXPLICIT_WAIT, 15);
    }

    public static int getRetryCount() {
        return ConfigReader.getInt(KEY_RETRY_COUNT, 1);
    }

    // -------------------------------------------------------------------------
    // Reporting
    // -------------------------------------------------------------------------

    public static boolean isScreenshotOnPass() {
        return ConfigReader.getBoolean(KEY_SCREENSHOT_ON_PASS, false);
    }

    public static String getReportDir() {
        return ConfigReader.get(KEY_REPORT_DIR, "reports");
    }

    // -------------------------------------------------------------------------
    // Computed helpers
    // -------------------------------------------------------------------------

    public static boolean isDatabaseConfigured() {
        return !getDbUrl().isBlank();
    }

    public static boolean isGridExecution() {
        return !getGridUrl().isBlank();
    }
}
