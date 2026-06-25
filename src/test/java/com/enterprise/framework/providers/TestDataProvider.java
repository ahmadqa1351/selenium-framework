package com.enterprise.framework.providers;

import com.enterprise.framework.utils.ExcelUtils;
import com.enterprise.framework.utils.JsonUtils;
import org.testng.annotations.DataProvider;

import java.util.Map;

/**
 * Centralized TestNG data providers.
 *
 * <p><b>Design:</b> All {@code @DataProvider} methods live here so that:
 * <ul>
 *   <li>Data sources are discovered in one place (easier maintenance)</li>
 *   <li>Multiple test classes can reference the same provider via
 *       {@code dataProviderClass = TestDataProvider.class}</li>
 *   <li>Switching data sources (Excel → API → DB) requires changes in one file only</li>
 * </ul>
 *
 * <p><b>Usage in test class:</b>
 * <pre>
 *   @Test(dataProvider = "validLoginData", dataProviderClass = TestDataProvider.class)
 *   public void testLogin(Map&lt;String, String&gt; data) {
 *       loginPage.loginAs(data.get("username"), data.get("password"));
 *   }
 * </pre>
 *
 * <p><b>Parallel data provider:</b> Set {@code parallel = true} on the @DataProvider
 * to run each data row in its own thread — combined with parallel test methods this
 * gives maximum throughput.
 */
public class TestDataProvider {

    // =========================================================================
    // Login data providers
    // =========================================================================

    /**
     * Valid credentials — expect successful login.
     * Source: {@code src/test/resources/testdata/login_data.json}
     */
    @DataProvider(name = "validLoginData", parallel = false)
    public static Object[][] validLoginData() {
        return JsonUtils.toTestNgDataProvider("testdata/login_data.json");
    }

    /**
     * Invalid credentials — expect login failure with error messages.
     * Source: inline (no file I/O — fast for small fixed datasets).
     */
    @DataProvider(name = "invalidLoginData")
    public static Object[][] invalidLoginData() {
        return new Object[][] {
            // { username, password, expectedError }
            { "wrong@email.com",    "ValidPass@1",   "Invalid username or password." },
            { "admin@company.com",  "wrongpass",     "Invalid username or password." },
            { "",                   "ValidPass@1",   "Username is required."          },
            { "admin@company.com",  "",              "Password is required."          },
            { "  ",                 "  ",            "Username is required."          },
            { "SQL' OR '1'='1",     "anything",     "Invalid username or password."  },
            { "<script>alert(1)</script>", "pass",  "Invalid username or password."  },
        };
    }

    /**
     * Boundary-value username tests.
     */
    @DataProvider(name = "usernameEdgeCases")
    public static Object[][] usernameEdgeCases() {
        return new Object[][] {
            { "a@b.co"     },   // Minimum valid email length
            { "user+tag@company.com" },  // Plus-address format
            { "USER@COMPANY.COM" },      // All uppercase (case-insensitive test)
            { "user.name.with.dots@company.co.uk" },  // Complex domain
        };
    }

    // =========================================================================
    // User management data providers
    // =========================================================================

    /**
     * User creation test data from Excel.
     * Source: {@code src/test/resources/testdata/users.xlsx} — sheet "CreateUser"
     */
    @DataProvider(name = "createUserData", parallel = true)
    public static Object[][] createUserData() {
        return ExcelUtils.toTestNgDataProvider("testdata/users.xlsx", "CreateUser");
    }

    /**
     * Role permission matrix — verifies each role sees correct menu items.
     */
    @DataProvider(name = "rolePermissions")
    public static Object[][] rolePermissions() {
        return new Object[][] {
            // { role,         canSeeUsers, canSeeReports, canSeeSettings }
            { "Admin",         true,        true,          true  },
            { "Manager",       false,       true,          false },
            { "Viewer",        false,       false,         false },
        };
    }

    // =========================================================================
    // Cross-browser data provider
    // =========================================================================

    /**
     * Used with smoke tests that must verify behavior on every supported browser.
     * Combine with test method to generate one run per browser.
     */
    @DataProvider(name = "browsers")
    public static Object[][] browsers() {
        return new Object[][] {
            { "chrome"  },
            { "firefox" },
            { "edge"    },
        };
    }

    // =========================================================================
    // Form field validation data providers
    // =========================================================================

    @DataProvider(name = "passwordValidationData")
    public static Object[][] passwordValidationData() {
        return new Object[][] {
            // { password,          isValid, expectedMessage }
            { "short",             false, "Password must be at least 8 characters." },
            { "alllowercase1!",    false, "Password must contain an uppercase letter." },
            { "ALLUPPERCASE1!",    false, "Password must contain a lowercase letter." },
            { "NoSpecialChar1",    false, "Password must contain a special character." },
            { "NoNumber!aA",       false, "Password must contain a number." },
            { "Valid@Pass1",       true,  ""                                          },
            { "Str0ng#P@ssword",   true,  ""                                          },
        };
    }

    // =========================================================================
    // API test data providers
    // =========================================================================

    @DataProvider(name = "apiEndpoints")
    public static Object[][] apiEndpoints() {
        return new Object[][] {
            // { endpoint,       method,   expectedStatus }
            { "/api/users",      "GET",    200 },
            { "/api/users/999",  "GET",    404 },
            { "/api/health",     "GET",    200 },
        };
    }
}
