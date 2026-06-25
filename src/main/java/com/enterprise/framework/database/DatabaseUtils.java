package com.enterprise.framework.database;

import com.enterprise.framework.config.EnvironmentConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.*;

/**
 * JDBC database validation utility.
 *
 * <p><b>Primary use cases in test automation:</b>
 * <ul>
 *   <li>Verify database state after UI actions (e.g., confirm a record was inserted)</li>
 *   <li>Seed test data directly into the DB before a test</li>
 *   <li>Clean up test data after a test run</li>
 *   <li>Read configuration or lookup values that are faster to retrieve from DB than via UI</li>
 * </ul>
 *
 * <p><b>Design:</b> One connection per test thread (lazy init on first use). The connection
 * is returned to a thread-local cache so repeated calls within the same test share one
 * connection. Call {@link #closeConnection()} in @AfterMethod if you use DB operations.
 *
 * <p><b>Security:</b> Always use parameterized queries via {@link PreparedStatement}.
 * String concatenation into SQL is never acceptable in production code.
 */
public final class DatabaseUtils {

    private static final Logger log = LogManager.getLogger(DatabaseUtils.class);

    /** One connection per thread. Prevents concurrent-access issues in parallel tests. */
    private static final ThreadLocal<Connection> connectionHolder = new ThreadLocal<>();

    private DatabaseUtils() { }

    // =========================================================================
    // Connection management
    // =========================================================================

    /**
     * Returns the database connection for the current thread.
     * A new connection is created on first access.
     */
    public static Connection getConnection() {
        Connection conn = connectionHolder.get();
        try {
            if (conn == null || conn.isClosed()) {
                conn = openConnection();
                connectionHolder.set(conn);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get database connection", e);
        }
        return conn;
    }

    private static Connection openConnection() {
        if (!EnvironmentConfig.isDatabaseConfigured()) {
            throw new IllegalStateException(
                "Database is not configured. Set db.url, db.username, db.password in your environment properties.");
        }
        String url      = EnvironmentConfig.getDbUrl();
        String username = EnvironmentConfig.getDbUsername();
        String password = EnvironmentConfig.getDbPassword();
        String driver   = EnvironmentConfig.getDbDriver();

        try {
            Class.forName(driver);
            Connection conn = DriverManager.getConnection(url, username, password);
            conn.setAutoCommit(true);
            log.info("Database connection established: {}", maskUrl(url));
            return conn;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("JDBC driver not found: " + driver, e);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to connect to database: " + maskUrl(url), e);
        }
    }

    /** Closes and removes the connection for the current thread. Call in @AfterMethod. */
    public static void closeConnection() {
        Connection conn = connectionHolder.get();
        if (conn != null) {
            try {
                conn.close();
                log.debug("Database connection closed for thread [{}]", Thread.currentThread().getName());
            } catch (SQLException e) {
                log.warn("Error closing database connection: {}", e.getMessage());
            } finally {
                connectionHolder.remove();
            }
        }
    }

    // =========================================================================
    // Query execution
    // =========================================================================

    /**
     * Executes a SELECT query and returns all rows as a list of maps.
     * Map key = column name (lowercase), value = cell value as String.
     *
     * <p><b>Usage:</b>
     * <pre>
     *   List&lt;Map&lt;String, String&gt;&gt; rows = DatabaseUtils.query(
     *       "SELECT * FROM users WHERE email = ?", "john@test.com");
     * </pre>
     */
    public static List<Map<String, String>> query(String sql, Object... params) {
        log.debug("Executing SQL: {}", sql);
        List<Map<String, String>> results = new ArrayList<>();

        try (PreparedStatement stmt = prepare(sql, params);
             ResultSet rs = stmt.executeQuery()) {

            ResultSetMetaData meta = rs.getMetaData();
            int colCount = meta.getColumnCount();

            while (rs.next()) {
                Map<String, String> row = new LinkedHashMap<>();
                for (int i = 1; i <= colCount; i++) {
                    String colName  = meta.getColumnLabel(i).toLowerCase();
                    String colValue = Objects.toString(rs.getString(i), "");
                    row.put(colName, colValue);
                }
                results.add(row);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Query execution failed: " + sql, e);
        }

        log.debug("Query returned {} row(s)", results.size());
        return results;
    }

    /**
     * Returns a single cell value as String.
     *
     * <p><b>Usage:</b>
     * <pre>
     *   String status = DatabaseUtils.querySingleValue(
     *       "SELECT status FROM orders WHERE id = ?", orderId);
     * </pre>
     */
    public static String querySingleValue(String sql, Object... params) {
        List<Map<String, String>> rows = query(sql, params);
        if (rows.isEmpty()) {
            throw new NoSuchElementException("Query returned no rows: " + sql);
        }
        Map<String, String> firstRow = rows.get(0);
        return firstRow.values().iterator().next();
    }

    /**
     * Returns the row count matching a condition.
     *
     * <p><b>Usage:</b>
     * <pre>
     *   int count = DatabaseUtils.queryCount("SELECT COUNT(*) FROM users WHERE active = ?", 1);
     * </pre>
     */
    public static int queryCount(String sql, Object... params) {
        String raw = querySingleValue(sql, params);
        return Integer.parseInt(raw);
    }

    /**
     * Executes an INSERT, UPDATE, or DELETE statement.
     *
     * @return number of rows affected
     */
    public static int execute(String sql, Object... params) {
        log.debug("Executing DML: {}", sql);
        try (PreparedStatement stmt = prepare(sql, params)) {
            int affected = stmt.executeUpdate();
            log.debug("DML affected {} row(s)", affected);
            return affected;
        } catch (SQLException e) {
            throw new RuntimeException("DML execution failed: " + sql, e);
        }
    }

    // =========================================================================
    // Assertion helpers — use these in tests for clean assertion syntax
    // =========================================================================

    /**
     * Asserts that a record exists matching the condition.
     *
     * <p><b>Usage:</b>
     * <pre>
     *   DatabaseUtils.assertRecordExists(
     *       "SELECT 1 FROM users WHERE email = ? AND status = ?", email, "ACTIVE");
     * </pre>
     */
    public static void assertRecordExists(String sql, Object... params) {
        int count = queryCount("SELECT COUNT(*) FROM (" + sql + ") t", params);
        if (count == 0) {
            throw new AssertionError("Expected a database record to exist, but none found. SQL: " + sql);
        }
    }

    public static void assertRecordDoesNotExist(String sql, Object... params) {
        int count = queryCount("SELECT COUNT(*) FROM (" + sql + ") t", params);
        if (count > 0) {
            throw new AssertionError("Expected no database records, but found " + count + ". SQL: " + sql);
        }
    }

    public static void assertFieldValue(String expectedValue, String sql, Object... params) {
        String actual = querySingleValue(sql, params);
        if (!expectedValue.equals(actual)) {
            throw new AssertionError(
                String.format("DB assertion failed. Expected: '%s', Actual: '%s'. SQL: %s",
                    expectedValue, actual, sql));
        }
    }

    // =========================================================================
    // Transaction helpers
    // =========================================================================

    public static void beginTransaction() throws SQLException {
        getConnection().setAutoCommit(false);
    }

    public static void commit() throws SQLException {
        getConnection().commit();
        getConnection().setAutoCommit(true);
    }

    public static void rollback() {
        try {
            getConnection().rollback();
            getConnection().setAutoCommit(true);
        } catch (SQLException e) {
            log.error("Rollback failed: {}", e.getMessage(), e);
        }
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private static PreparedStatement prepare(String sql, Object[] params) throws SQLException {
        PreparedStatement stmt = getConnection().prepareStatement(sql);
        for (int i = 0; i < params.length; i++) {
            stmt.setObject(i + 1, params[i]);
        }
        return stmt;
    }

    /** Masks the password portion of a JDBC URL for safe logging. */
    private static String maskUrl(String url) {
        return url.replaceAll("password=[^&;]*", "password=***");
    }
}
