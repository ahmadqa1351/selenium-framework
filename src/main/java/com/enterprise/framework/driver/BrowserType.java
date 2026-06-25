package com.enterprise.framework.driver;

/**
 * Enum representing all supported browser types.
 *
 * <p>Design decision: Using an enum instead of raw strings eliminates typos,
 * enables IDE auto-complete, and makes switch expressions exhaustive. New browsers
 * are added here first — the rest of the framework picks them up automatically.
 *
 * <p>The {@code fromString()} factory method allows external configuration (CI flags,
 * properties files) to resolve a browser name to its enum constant safely.
 */
public enum BrowserType {

    CHROME("chrome"),
    FIREFOX("firefox"),
    EDGE("edge"),
    SAFARI("safari"),
    CHROME_HEADLESS("chrome_headless"),
    FIREFOX_HEADLESS("firefox_headless"),
    EDGE_HEADLESS("edge_headless");

    private final String value;

    BrowserType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    /**
     * Case-insensitive lookup from a raw string (e.g. from a properties file or -D flag).
     *
     * @param browser raw browser name
     * @return matched BrowserType
     * @throws IllegalArgumentException if no match is found
     */
    public static BrowserType fromString(String browser) {
        if (browser == null || browser.isBlank()) {
            throw new IllegalArgumentException("Browser name must not be null or blank.");
        }
        for (BrowserType type : values()) {
            if (type.value.equalsIgnoreCase(browser.trim())) {
                return type;
            }
        }
        throw new IllegalArgumentException(
            String.format("Unsupported browser: '%s'. Valid values: chrome, firefox, edge, " +
                "safari, chrome_headless, firefox_headless, edge_headless", browser));
    }

    /** Returns true if this is any headless variant. */
    public boolean isHeadless() {
        return this == CHROME_HEADLESS || this == FIREFOX_HEADLESS || this == EDGE_HEADLESS;
    }

    /** Returns the base browser for a headless variant (e.g. CHROME_HEADLESS → CHROME). */
    public BrowserType toHeadless() {
        return switch (this) {
            case CHROME         -> CHROME_HEADLESS;
            case FIREFOX        -> FIREFOX_HEADLESS;
            case EDGE           -> EDGE_HEADLESS;
            default             -> this;
        };
    }
}
