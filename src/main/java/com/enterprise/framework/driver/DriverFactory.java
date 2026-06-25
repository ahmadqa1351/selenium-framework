package com.enterprise.framework.driver;

import com.enterprise.framework.config.ConfigReader;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.safari.SafariDriver;
import org.openqa.selenium.safari.SafariOptions;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Factory responsible for creating configured WebDriver instances.
 *
 * <p><b>Design:</b> Combines the Factory and Strategy patterns. The factory selects
 * the correct construction strategy (local vs remote, Chrome vs Firefox, etc.) based
 * on configuration. Callers never construct drivers directly — they call
 * {@link #createDriver(BrowserType)} and receive a fully configured, ready-to-use driver.
 *
 * <p><b>Extension points:</b>
 * <ul>
 *   <li>Add a new browser: add a case to the switch in {@code createLocalDriver()}</li>
 *   <li>Add cloud execution (BrowserStack / Sauce Labs): add an {@code if} branch in
 *       {@code createDriver()} before the remote check</li>
 *   <li>Change options per environment: inject environment logic inside the
 *       {@code build*Options()} methods</li>
 * </ul>
 */
public final class DriverFactory {

    private static final Logger log = LogManager.getLogger(DriverFactory.class);

    private static final int IMPLICIT_WAIT_SEC  = ConfigReader.getInt("implicit.wait.seconds", 0);
    private static final int PAGE_LOAD_TIMEOUT  = ConfigReader.getInt("page.load.timeout.seconds", 30);
    private static final int SCRIPT_TIMEOUT     = ConfigReader.getInt("script.timeout.seconds", 30);
    private static final String DOWNLOAD_DIR    = System.getProperty("user.dir") + "/downloads";

    private DriverFactory() { }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Creates and returns a fully configured {@link WebDriver}.
     *
     * <p>Routing logic:
     * <ol>
     *   <li>If {@code grid.url} is set in config → create {@link RemoteWebDriver}</li>
     *   <li>Otherwise → create a local driver managed by WebDriverManager</li>
     * </ol>
     *
     * @param browserType the browser to launch
     * @return ready-to-use WebDriver instance
     */
    public static WebDriver createDriver(BrowserType browserType) {
        String gridUrl = ConfigReader.get("grid.url", "");
        WebDriver driver;

        if (!gridUrl.isBlank()) {
            log.info("Creating REMOTE driver [{}] on grid: {}", browserType, gridUrl);
            driver = createRemoteDriver(browserType, gridUrl);
        } else {
            log.info("Creating LOCAL driver [{}] on thread [{}]", browserType, Thread.currentThread().getName());
            driver = createLocalDriver(browserType);
        }

        applyTimeouts(driver);
        driver.manage().window().maximize();
        log.debug("WebDriver ready — implicit wait={}s, page load={}s, script={}s",
            IMPLICIT_WAIT_SEC, PAGE_LOAD_TIMEOUT, SCRIPT_TIMEOUT);
        return driver;
    }

    // -------------------------------------------------------------------------
    // Local driver creation
    // -------------------------------------------------------------------------

    private static WebDriver createLocalDriver(BrowserType browserType) {
        return switch (browserType) {
            case CHROME         -> { WebDriverManager.chromedriver().setup();  yield new ChromeDriver(chromeOptions(false)); }
            case CHROME_HEADLESS-> { WebDriverManager.chromedriver().setup();  yield new ChromeDriver(chromeOptions(true));  }
            case FIREFOX        -> { WebDriverManager.firefoxdriver().setup(); yield new FirefoxDriver(firefoxOptions(false)); }
            case FIREFOX_HEADLESS->{ WebDriverManager.firefoxdriver().setup(); yield new FirefoxDriver(firefoxOptions(true));  }
            case EDGE           -> { WebDriverManager.edgedriver().setup();    yield new EdgeDriver(edgeOptions(false));  }
            case EDGE_HEADLESS  -> { WebDriverManager.edgedriver().setup();    yield new EdgeDriver(edgeOptions(true));   }
            case SAFARI         ->   new SafariDriver(safariOptions());
        };
    }

    // -------------------------------------------------------------------------
    // Remote / Selenium Grid driver creation
    // -------------------------------------------------------------------------

    private static WebDriver createRemoteDriver(BrowserType browserType, String gridUrl) {
        Capabilities capabilities = switch (browserType) {
            case CHROME, CHROME_HEADLESS    -> chromeOptions(browserType.isHeadless());
            case FIREFOX, FIREFOX_HEADLESS  -> firefoxOptions(browserType.isHeadless());
            case EDGE, EDGE_HEADLESS        -> edgeOptions(browserType.isHeadless());
            case SAFARI                     -> safariOptions();
        };

        try {
            return new RemoteWebDriver(new URL(gridUrl), capabilities);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid Selenium Grid URL: " + gridUrl, e);
        }
    }

    // -------------------------------------------------------------------------
    // Browser options builders
    // -------------------------------------------------------------------------

    private static ChromeOptions chromeOptions(boolean headless) {
        ChromeOptions options = new ChromeOptions();

        if (headless) {
            // --headless=new uses the modern headless mode (Chrome 112+)
            options.addArguments("--headless=new");
        }

        options.addArguments(
            "--no-sandbox",               // Required in Docker / CI environments
            "--disable-dev-shm-usage",    // Prevents crashes in low-memory Docker containers
            "--disable-gpu",
            "--window-size=1920,1080",
            "--disable-extensions",
            "--disable-infobars",
            "--disable-notifications",
            "--disable-popup-blocking",
            "--remote-allow-origins=*",   // Required for Selenium 4 + ChromeDriver 111+
            "--lang=en-US"
        );

        // Preferences: suppress popups, set download folder
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("profile.default_content_setting_values.notifications", 2);
        prefs.put("download.default_directory", DOWNLOAD_DIR);
        prefs.put("download.prompt_for_download", false);
        prefs.put("plugins.always_open_pdf_externally", true);
        options.setExperimentalOption("prefs", prefs);

        // Exclude the "Chrome is being controlled by automated software" bar
        options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
        options.setExperimentalOption("useAutomationExtension", false);

        return options;
    }

    private static FirefoxOptions firefoxOptions(boolean headless) {
        FirefoxOptions options = new FirefoxOptions();

        if (headless) {
            options.addArguments("-headless");
        }

        options.addArguments("--width=1920", "--height=1080");
        options.addPreference("browser.download.folderList", 2);
        options.addPreference("browser.download.dir", DOWNLOAD_DIR);
        options.addPreference("browser.helperApps.neverAsk.saveToDisk",
            "application/pdf,application/octet-stream,application/vnd.ms-excel");
        options.addPreference("pdfjs.disabled", true);
        return options;
    }

    private static EdgeOptions edgeOptions(boolean headless) {
        EdgeOptions options = new EdgeOptions();

        if (headless) {
            options.addArguments("--headless=new");
        }

        options.addArguments(
            "--no-sandbox",
            "--disable-dev-shm-usage",
            "--window-size=1920,1080",
            "--disable-extensions"
        );
        return options;
    }

    private static SafariOptions safariOptions() {
        SafariOptions options = new SafariOptions();
        options.setAutomaticInspection(false);
        return options;
    }

    // -------------------------------------------------------------------------
    // Driver configuration helpers
    // -------------------------------------------------------------------------

    private static void applyTimeouts(WebDriver driver) {
        // NOTE: Most frameworks set implicit wait here. The recommendation is to
        // keep implicit wait = 0 and rely solely on explicit waits (WaitUtils) to
        // avoid unpredictable interactions between the two wait mechanisms.
        if (IMPLICIT_WAIT_SEC > 0) {
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(IMPLICIT_WAIT_SEC));
        }
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(PAGE_LOAD_TIMEOUT));
        driver.manage().timeouts().scriptTimeout(Duration.ofSeconds(SCRIPT_TIMEOUT));
    }
}
