# Enterprise Selenium WebDriver Framework

> Production-grade, scalable test automation framework for large enterprise QA organizations.
> Built with Java 17, Selenium 4, TestNG, Maven, Extent Reports, and Log4j2.

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Project Structure](#project-structure)
3. [Quick Start](#quick-start)
4. [Configuration Management](#configuration-management)
5. [Running Tests](#running-tests)
6. [Writing Tests](#writing-tests)
7. [Page Object Model](#page-object-model)
8. [Test Data Management](#test-data-management)
9. [Parallel Execution](#parallel-execution)
10. [Reporting](#reporting)
11. [CI/CD Integration](#cicd-integration)
12. [Docker & Selenium Grid](#docker--selenium-grid)
13. [Extending the Framework](#extending-the-framework)
14. [Troubleshooting](#troubleshooting)

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                        TEST LAYER                               │
│   LoginSmokeTest  │  LoginRegressionTest  │  UserManagementTest │
├─────────────────────────────────────────────────────────────────┤
│                       BASE TEST LAYER                           │
│  BaseTest (@Before/@After, driver lifecycle, page factory)      │
├──────────────────────────┬──────────────────────────────────────┤
│      PAGE OBJECT LAYER   │       UTILITY LAYER                  │
│  BasePage → LoginPage    │  WaitUtils  │  ElementUtils          │
│  BasePage → DashboardPage│  ExcelUtils │  JsonUtils             │
│  NavigationBar (component│  DateUtils  │  ScreenshotUtils       │
├──────────────────────────┴──────────────────────────────────────┤
│                      DRIVER LAYER                               │
│     DriverFactory (creates)  │  DriverManager (ThreadLocal)     │
├─────────────────────────────────────────────────────────────────┤
│                    REPORTING & LISTENERS                        │
│   ExtentReportManager  │  TestListener  │  RetryAnalyzer        │
├─────────────────────────────────────────────────────────────────┤
│                  INFRASTRUCTURE LAYER                           │
│   ConfigReader  │  EnvironmentConfig  │  RestApiClient          │
│   DatabaseUtils │  Log4j2             │  WebDriverManager       │
└─────────────────────────────────────────────────────────────────┘
```

---

## Project Structure

```
selenium-framework/
├── src/main/java/com/enterprise/framework/
│   ├── driver/
│   │   ├── BrowserType.java          # Enum: chrome, firefox, edge, headless variants
│   │   ├── DriverManager.java        # ThreadLocal WebDriver — the parallel safety core
│   │   └── DriverFactory.java        # Creates local & remote (Grid) drivers
│   ├── config/
│   │   ├── ConfigReader.java         # Layered config: base → env → system props
│   │   └── EnvironmentConfig.java    # Typed accessors for all config values
│   ├── base/
│   │   ├── BasePage.java             # Abstract POM base with full Selenium wrappers
│   │   └── BaseTest.java             # Abstract test base with @Before/@After lifecycle
│   ├── utils/
│   │   ├── ElementUtils.java         # Resilient element interactions with retry
│   │   ├── WaitUtils.java            # Explicit wait strategies (no implicit waits)
│   │   ├── ScreenshotUtils.java      # File, Base64, and byte screenshot capture
│   │   ├── ExcelUtils.java           # Apache POI Excel reader for data-driven tests
│   │   ├── JsonUtils.java            # Gson JSON reader with path navigation
│   │   └── DateUtils.java            # Timestamps, date arithmetic, unique test data
│   ├── reporting/
│   │   └── ExtentReportManager.java  # Thread-safe singleton Extent Reports manager
│   ├── listeners/
│   │   ├── TestListener.java         # TestNG listener: reports, screenshots, retry
│   │   └── RetryAnalyzer.java        # Configurable test retry (retry.count=N)
│   ├── api/
│   │   └── RestApiClient.java        # RestAssured wrapper for API setup/validation
│   ├── database/
│   │   └── DatabaseUtils.java        # JDBC utility for DB assertion and seeding
│   └── pages/
│       ├── LoginPage.java            # Sample production Page Object
│       ├── DashboardPage.java        # Sample post-login page
│       └── NavigationBar.java        # Shared nav component (Component Object Model)
│
├── src/test/java/com/enterprise/framework/
│   ├── tests/
│   │   ├── smoke/
│   │   │   └── LoginSmokeTest.java
│   │   └── regression/
│   │       ├── LoginRegressionTest.java
│   │       └── UserManagementTest.java
│   └── providers/
│       └── TestDataProvider.java     # All @DataProvider methods centralized here
│
├── src/main/resources/
│   ├── config.properties             # Base defaults
│   ├── dev.properties                # DEV overrides
│   ├── qa.properties                 # QA overrides (default)
│   ├── uat.properties                # UAT overrides
│   └── log4j2.xml                    # Log4j2: colored console + rolling file appenders
│
├── src/test/resources/testdata/
│   ├── login_data.json               # JSON test data for login tests
│   └── users.xlsx                    # Excel test data for user management tests
│
├── testng.xml                        # Full suite (sequential)
├── testng-smoke.xml                  # Smoke only (<5 min)
├── testng-regression.xml             # Regression (parallel by class)
├── testng-parallel.xml               # Full parallel (by method)
├── pom.xml                           # Maven: dependencies, plugins, profiles
├── Jenkinsfile                       # Jenkins declarative pipeline
├── Dockerfile                        # Portable test execution image
├── docker-compose.yml                # Selenium Grid topology
└── .github/workflows/ci.yml          # GitHub Actions CI pipeline
```

---

## Quick Start

### Prerequisites
- Java 17+
- Maven 3.9+
- Chrome/Firefox/Edge (managed automatically by WebDriverManager)

### Clone and run
```bash
git clone https://github.com/your-org/selenium-framework.git
cd selenium-framework

# Run smoke tests (Chrome, QA environment)
mvn test -P smoke -Denv=qa -Dbrowser=chrome

# Run smoke tests (headless — for CI)
mvn test -P smoke -Denv=qa -Dbrowser=chrome_headless

# Run full regression
mvn test -P regression -Denv=qa -Dbrowser=chrome
```

---

## Configuration Management

### How it works

Configuration is loaded in priority order — each layer overrides the previous:

```
1. config.properties       ← base defaults
2. {env}.properties        ← environment overrides (-Denv=qa)
3. JVM system properties   ← CI/CD flags (-Dbrowser=firefox)
```

### Adding a new environment

```bash
# 1. Create the file
touch src/main/resources/staging.properties

# 2. Add environment-specific values
echo "base.url=https://app-staging.company.com" >> src/main/resources/staging.properties

# 3. Run with it
mvn test -Denv=staging
```

### Sensitive values in CI

Never commit passwords to version control. Pass them as environment variables:

```bash
# Jenkins: stored in credential store, injected as -D flags
mvn test -Dadmin.password=${QA_ADMIN_PASSWORD}

# GitHub Actions: stored in repository secrets
mvn test -Dadmin.password=${{ secrets.QA_ADMIN_PASSWORD }}
```

---

## Running Tests

### By profile (recommended)

| Profile      | Command                              | Purpose                      |
|--------------|--------------------------------------|------------------------------|
| smoke        | `mvn test -P smoke`                  | CI gate, <5 min              |
| regression   | `mvn test -P regression`             | Nightly full suite           |
| parallel     | `mvn test -P parallel`               | Max throughput, Grid required|
| headless     | `mvn test -P headless`               | CI headless Chrome           |

### By group

```bash
# Smoke group only
mvn test -Dgroups=smoke

# Security tests only
mvn test -Dgroups=security

# Exclude flaky tests
mvn test -DexcludedGroups=data-driven
```

### Cross-environment

```bash
# DEV environment, Firefox
mvn test -P smoke -Denv=dev -Dbrowser=firefox

# UAT, headless, smoke only
mvn test -P smoke,headless -Denv=uat

# Selenium Grid execution
mvn test -P parallel -Dgrid.url=http://selenium-hub:4444/wd/hub
```

---

## Writing Tests

### Minimal test class

```java
public class MyFeatureTest extends BaseTest {

    @Test(groups = {"smoke"}, description = "TC-XX-001: My feature works")
    public void myFeatureWorks() {
        step("Step 1: Navigate to the feature");
        MyFeaturePage page = getPage(MyFeaturePage.class);

        step("Step 2: Perform an action");
        page.doSomething();

        step("Step 3: Assert outcome");
        Assert.assertTrue(page.isResultVisible(), "Result should be visible");
        stepPass("TC-XX-001 PASSED");
    }
}
```

### Test with API setup (recommended pattern)

```java
@BeforeMethod
public void setupViaApi() {
    apiClient = new RestApiClient();
    token = apiClient.loginAndGetToken(adminUser, adminPass);
    testData = apiClient.withBearerToken(token).post("/api/seed", payload);
}

@Test
public void verifyDataInUI() {
    // Use UI only for the behavior being tested
    // Use API for setup and verification of side effects
}

@AfterMethod
public void cleanupViaApi() {
    apiClient.withBearerToken(token).delete("/api/cleanup/" + testDataId);
}
```

### Data-driven test

```java
@Test(
    dataProvider      = "validLoginData",
    dataProviderClass = TestDataProvider.class
)
public void loginWithMultipleUsers(Map<String, String> data) {
    LoginPage loginPage = getPage(LoginPage.class);
    DashboardPage dashboard = loginPage.loginAs(
        data.get("username"),
        data.get("password")
    );
    Assert.assertTrue(dashboard.isLoaded());
}
```

---

## Page Object Model

### Creating a new page object

```java
public class MyPage extends BasePage {

    // 1. Locators as constants — never inline strings in methods
    private static final By SUBMIT_BTN    = By.id("submitBtn");
    private static final By RESULT_TABLE  = By.cssSelector(".results-table");

    // 2. PageFactory elements for complex locators
    @FindBy(css = ".filter-dropdown")
    private WebElement filterDropdown;

    // 3. Constructor — always call super(driver)
    public MyPage(WebDriver driver) {
        super(driver);
    }

    // 4. isLoaded() — verify page is ready
    @Override
    public boolean isLoaded() {
        return isPresent(SUBMIT_BTN) && getPageUrl().contains("/my-page");
    }

    // 5. Action methods — return this or next page, never void
    public MyPage applyFilter(String filterValue) {
        step("Applying filter: " + filterValue);
        selectByText(By.cssSelector(".filter-dropdown"), filterValue);
        click(SUBMIT_BTN);
        return this;
    }

    // 6. State readers — used in test assertions
    public int getResultCount() {
        return countElements(By.cssSelector(".results-table tr:not(.header)"));
    }
}
```

---

## Test Data Management

### JSON (recommended for structured test data)

```json
// src/test/resources/testdata/my_data.json
[
  { "username": "user1@test.com", "role": "Admin" },
  { "username": "user2@test.com", "role": "Viewer" }
]
```

```java
// In TestDataProvider.java
@DataProvider(name = "myData")
public static Object[][] myData() {
    return JsonUtils.toTestNgDataProvider("testdata/my_data.json");
}
```

### Excel (for larger datasets or business-analyst-managed data)

```java
@DataProvider(name = "excelData")
public static Object[][] excelData() {
    return ExcelUtils.toTestNgDataProvider("testdata/users.xlsx", "Sheet1");
}
```

### Inline (for small fixed datasets)

```java
@DataProvider(name = "roles")
public static Object[][] roles() {
    return new Object[][] {
        { "Admin",   true  },
        { "Viewer",  false },
    };
}
```

---

## Parallel Execution

The framework is parallel-safe by design:

- `DriverManager` — ThreadLocal WebDriver, one per thread
- `ExtentReportManager` — ThreadLocal ExtentTest, one per thread
- `RetryAnalyzer` — ThreadLocal retry counter, one per thread

### Enable parallel execution

```xml
<!-- testng-parallel.xml -->
<suite parallel="methods" thread-count="4">
```

```bash
mvn test -P parallel -Dparallel.thread.count=4
```

### With Selenium Grid (recommended for parallel > 4)

```bash
# Start the Grid
docker-compose up -d selenium-hub chrome firefox

# Run tests against it
mvn test -P parallel -Dgrid.url=http://localhost:4444/wd/hub -Dthread.count=8
```

---

## Reporting

Reports are written to `reports/` after each execution:

```
reports/
├── extent-report_2024-01-15_14-30-00.html   ← Open this in browser
├── screenshots/
│   └── LoginSmokeTest.validAdminLogin_20240115_143023_456.png
└── logs/
    ├── framework.log
    ├── test-execution.log
    └── errors.log
```

Open the HTML report in any browser. It includes:
- Pass/fail/skip summary with percentages
- Per-test timeline and duration
- Inline failure screenshots
- System info (env, browser, OS)
- Test category breakdown (Smoke, Regression)

---

## CI/CD Integration

### GitHub Actions

Push to `main` or `develop` → Smoke suite runs automatically.  
Nightly at 02:00 UTC → Full regression runs.

```yaml
# Manual trigger with parameters
gh workflow run ci.yml \
  -f suite=regression \
  -f environment=uat \
  -f browser=chrome_headless
```

### Jenkins

```bash
# Build with parameters from CLI
curl -X POST "http://jenkins/job/selenium-framework/buildWithParameters" \
  --user admin:token \
  --data "ENV=qa&BROWSER=chrome_headless&SUITE=regression"
```

---

## Docker & Selenium Grid

```bash
# Start Grid infrastructure
docker-compose up -d selenium-hub chrome firefox

# Grid console → http://localhost:4444

# Scale Chrome nodes for heavy load
docker-compose up -d --scale chrome=4

# Run framework in Docker against the Grid
docker-compose run --rm \
  -e ENV=qa \
  -e SUITE=regression \
  -e THREAD_COUNT=8 \
  -e ADMIN_PASSWORD=secret \
  framework-runner

# Retrieve reports from the container
docker cp framework-runner:/app/reports ./reports
```

---

## Extending the Framework

### Add a new browser

1. Add to `BrowserType.java`: `OPERA("opera")`
2. Add case to `DriverFactory.createLocalDriver()`:
   ```java
   case OPERA -> { WebDriverManager.operadriver().setup(); yield new OperaDriver(); }
   ```

### Add a new environment

1. Create `src/main/resources/staging.properties`
2. Run with `-Denv=staging`

### Add cloud execution (BrowserStack / Sauce Labs)

1. In `DriverFactory.createDriver()`, add a check before the Grid check:
   ```java
   if (!ConfigReader.get("browserstack.username").isBlank()) {
       return createBrowserStackDriver(browserType);
   }
   ```
2. Implement `createBrowserStackDriver()` using BrowserStack's RemoteWebDriver URL.

### Add a new report type (Allure)

1. Add `allure-testng` dependency to `pom.xml`
2. Add `@Epic`, `@Feature`, `@Story` annotations to test classes
3. Run: `mvn allure:serve`

---

## Troubleshooting

| Symptom | Likely Cause | Fix |
|---------|-------------|-----|
| `WebDriver is not initialized` | `@BeforeMethod` didn't run | Check class extends `BaseTest` |
| `Element not clickable` | Overlay/animation | Use `jsClick()` or increase wait |
| `StaleElementReferenceException` | DOM updated after fetch | `ElementUtils` retries automatically |
| Parallel tests interfering | Shared static state | Ensure no static WebDriver fields |
| Grid connection refused | Grid not started | `docker-compose up -d selenium-hub chrome` |
| `Required property missing` | Config key not in .properties | Add key to `{env}.properties` |
| Report not generated | Suite listener not registered | Add `<listener>` to testng.xml |

---

## Dependency Versions

| Library | Version | Purpose |
|---------|---------|---------|
| Selenium WebDriver | 4.21.0 | Browser automation |
| TestNG | 7.10.2 | Test framework |
| WebDriverManager | 5.8.0 | Driver binary management |
| Extent Reports | 5.1.1 | HTML test reports |
| Log4j2 | 2.23.1 | Structured logging |
| RestAssured | 5.4.0 | API testing |
| Apache POI | 5.2.5 | Excel data reading |
| Gson | 2.11.0 | JSON parsing |
| Jackson | 2.17.1 | JSON serialization |
| MySQL Connector | 8.3.0 | Database access |

---

*Framework maintained by the Enterprise QA Platform team. Raise issues via JIRA project QAF.*
