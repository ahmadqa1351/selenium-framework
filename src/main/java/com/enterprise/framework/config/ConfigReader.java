package com.enterprise.framework.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Centralized, thread-safe configuration reader.
 *
 * <p><b>Loading order (later sources win):</b>
 * <ol>
 *   <li>{@code config.properties} — base defaults (always loaded)</li>
 *   <li>{@code {env}.properties} — environment overrides (loaded from {@code -Denv=qa})</li>
 *   <li>JVM system properties ({@code -Dbrowser=firefox}) — highest priority</li>
 * </ol>
 *
 * <p><b>Design decision:</b> Properties are loaded once at class-load time (static initializer)
 * so they're available everywhere without passing a config object around. This is intentional:
 * config is read-only and shared; it doesn't need dependency injection.
 *
 * <p><b>Adding a new environment:</b> Add {@code prod.properties} to
 * {@code src/main/resources/} and run with {@code -Denv=prod}.
 */
public final class ConfigReader {

    private static final Logger log = LogManager.getLogger(ConfigReader.class);
    private static final Properties props = new Properties();

    static {
        // Step 1: Load the base config
        loadFile("config.properties");

        // Step 2: Layer environment-specific overrides
        String env = resolveEnv();
        String envFile = env + ".properties";
        loadFile(envFile);

        log.info("Configuration loaded. Active environment: [{}]", env.toUpperCase());
    }

    private ConfigReader() { }

    // -------------------------------------------------------------------------
    // Core resolution
    // -------------------------------------------------------------------------

    /**
     * Resolves a string property.
     *
     * <p>Lookup order: system property → loaded properties file → {@code defaultValue}
     *
     * @param key          property key
     * @param defaultValue fallback if key is absent
     * @return resolved string value, never null
     */
    public static String get(String key, String defaultValue) {
        // JVM system properties (-D flags) always win — enables CI override without
        // editing any file
        String sysProp = System.getProperty(key);
        if (sysProp != null && !sysProp.isBlank()) {
            return sysProp.trim();
        }
        return props.getProperty(key, defaultValue).trim();
    }

    /** Gets a required property — throws if absent or blank. */
    public static String getRequired(String key) {
        String value = get(key, "");
        if (value.isBlank()) {
            throw new IllegalStateException(
                "Required configuration property '" + key + "' is missing. " +
                "Check config.properties or the active environment properties file.");
        }
        return value;
    }

    /** Gets a property, returning empty string if absent. */
    public static String get(String key) {
        return get(key, "");
    }

    // -------------------------------------------------------------------------
    // Typed getters
    // -------------------------------------------------------------------------

    public static int getInt(String key, int defaultValue) {
        String raw = get(key, String.valueOf(defaultValue));
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            log.warn("Property '{}' value '{}' is not a valid integer. Using default: {}", key, raw, defaultValue);
            return defaultValue;
        }
    }

    public static long getLong(String key, long defaultValue) {
        String raw = get(key, String.valueOf(defaultValue));
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            log.warn("Property '{}' value '{}' is not a valid long. Using default: {}", key, raw, defaultValue);
            return defaultValue;
        }
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        String raw = get(key, String.valueOf(defaultValue));
        return Boolean.parseBoolean(raw);
    }

    // -------------------------------------------------------------------------
    // Convenience shortcuts for the most common keys
    // -------------------------------------------------------------------------

    public static String getBaseUrl() {
        return getRequired("base.url");
    }

    public static String getEnvironment() {
        return resolveEnv();
    }

    public static String getBrowser() {
        return get("browser", "chrome");
    }

    public static String getGridUrl() {
        return get("grid.url", "");
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private static String resolveEnv() {
        // Priority: -Denv=qa → properties file value → "qa" (safe default)
        String env = System.getProperty("env");
        if (env == null || env.isBlank()) {
            env = props.getProperty("env", "qa");
        }
        return env.trim().toLowerCase();
    }

    private static void loadFile(String fileName) {
        try (InputStream is = ConfigReader.class.getClassLoader().getResourceAsStream(fileName)) {
            if (is == null) {
                log.warn("Configuration file '{}' not found on classpath — skipping.", fileName);
                return;
            }
            Properties fileProps = new Properties();
            fileProps.load(is);
            // Merge: fileProps overrides any existing keys (environment beats base)
            props.putAll(fileProps);
            log.debug("Loaded configuration from: {}", fileName);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load configuration file: " + fileName, e);
        }
    }
}
