package com.enterprise.framework.api;

import com.enterprise.framework.config.EnvironmentConfig;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

/**
 * Reusable REST API client built on RestAssured.
 *
 * <p><b>Use cases in UI automation:</b>
 * <ul>
 *   <li>API login to obtain auth tokens before UI tests (faster than UI login)</li>
 *   <li>Seed test data via API before a UI test starts</li>
 *   <li>Clean up test data via API after a test completes</li>
 *   <li>Validate backend state after a UI action (DB record created, email sent, etc.)</li>
 *   <li>Standalone API regression tests alongside UI tests</li>
 * </ul>
 *
 * <p><b>Design:</b> Builder-like API. Callers chain method calls to build and
 * execute requests:
 * <pre>
 *   Response response = new RestApiClient()
 *       .withBearerToken(token)
 *       .post("/api/users", payload);
 * </pre>
 */
public class RestApiClient {

    private static final Logger log = LogManager.getLogger(RestApiClient.class);

    private final RequestSpecification requestSpec;
    private String baseUri;

    // =========================================================================
    // Constructors / initialization
    // =========================================================================

    public RestApiClient() {
        this(EnvironmentConfig.getApiBaseUrl());
    }

    public RestApiClient(String baseUri) {
        this.baseUri = baseUri;
        this.requestSpec = buildDefaultSpec(baseUri);
    }

    private static RequestSpecification buildDefaultSpec(String baseUri) {
        return new RequestSpecBuilder()
            .setBaseUri(baseUri)
            .setContentType(ContentType.JSON)
            .setAccept(ContentType.JSON)
            .log(LogDetail.ALL)
            .build();
    }

    // =========================================================================
    // Authentication configurators (fluent)
    // =========================================================================

    public RestApiClient withBearerToken(String token) {
        requestSpec.header("Authorization", "Bearer " + token);
        return this;
    }

    public RestApiClient withBasicAuth(String username, String password) {
        requestSpec.auth().preemptive().basic(username, password);
        return this;
    }

    public RestApiClient withApiKey(String headerName, String apiKey) {
        requestSpec.header(headerName, apiKey);
        return this;
    }

    public RestApiClient withHeader(String name, String value) {
        requestSpec.header(name, value);
        return this;
    }

    public RestApiClient withHeaders(Map<String, String> headers) {
        headers.forEach(requestSpec::header);
        return this;
    }

    public RestApiClient withQueryParam(String name, Object value) {
        requestSpec.queryParam(name, value);
        return this;
    }

    public RestApiClient withQueryParams(Map<String, Object> params) {
        params.forEach(requestSpec::queryParam);
        return this;
    }

    // =========================================================================
    // HTTP verb methods
    // =========================================================================

    public Response get(String endpoint) {
        log.info("GET  → {}{}", baseUri, endpoint);
        Response response = RestAssured.given(requestSpec)
            .when().get(endpoint)
            .then().log().all().extract().response();
        logResponse(response);
        return response;
    }

    public Response post(String endpoint, Object body) {
        log.info("POST → {}{}", baseUri, endpoint);
        Response response = RestAssured.given(requestSpec)
            .body(body)
            .when().post(endpoint)
            .then().log().all().extract().response();
        logResponse(response);
        return response;
    }

    public Response put(String endpoint, Object body) {
        log.info("PUT  → {}{}", baseUri, endpoint);
        Response response = RestAssured.given(requestSpec)
            .body(body)
            .when().put(endpoint)
            .then().log().all().extract().response();
        logResponse(response);
        return response;
    }

    public Response patch(String endpoint, Object body) {
        log.info("PATCH → {}{}", baseUri, endpoint);
        Response response = RestAssured.given(requestSpec)
            .body(body)
            .when().patch(endpoint)
            .then().log().all().extract().response();
        logResponse(response);
        return response;
    }

    public Response delete(String endpoint) {
        log.info("DELETE → {}{}", baseUri, endpoint);
        Response response = RestAssured.given(requestSpec)
            .when().delete(endpoint)
            .then().log().all().extract().response();
        logResponse(response);
        return response;
    }

    // =========================================================================
    // Common authentication flow helpers
    // =========================================================================

    /**
     * Performs a standard username/password login and returns the bearer token.
     * Adjust the JSON keys and response path to match your API's contract.
     */
    public String loginAndGetToken(String username, String password) {
        Map<String, String> credentials = Map.of(
            "username", username,
            "password", password
        );
        Response response = post("/auth/login", credentials);
        assertStatusCode(response, 200);
        String token = response.jsonPath().getString("data.token");
        if (token == null || token.isBlank()) {
            throw new RuntimeException("Login API did not return a token. Response: " + response.asString());
        }
        log.info("API login successful. Token acquired.");
        return token;
    }

    // =========================================================================
    // Response assertion helpers
    // =========================================================================

    public void assertStatusCode(Response response, int expectedCode) {
        int actual = response.getStatusCode();
        if (actual != expectedCode) {
            throw new AssertionError(
                String.format("Expected HTTP %d but got %d. Body: %s",
                    expectedCode, actual, response.asString()));
        }
        log.debug("Status code assertion passed: {}", actual);
    }

    public <T> T extractField(Response response, String jsonPath, Class<T> type) {
        T value = response.jsonPath().getObject(jsonPath, type);
        log.debug("Extracted '{}' = {}", jsonPath, value);
        return value;
    }

    public String extractString(Response response, String jsonPath) {
        return extractField(response, jsonPath, String.class);
    }

    public int extractInt(Response response, String jsonPath) {
        return response.jsonPath().getInt(jsonPath);
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private void logResponse(Response response) {
        log.debug("Response → Status: {} | Time: {}ms | Size: {} bytes",
            response.getStatusCode(),
            response.getTime(),
            response.asByteArray().length);
    }

    /**
     * Builds a shared ResponseSpecification for reusable assertions.
     * Example: {@code RestAssured.responseSpecification = RestApiClient.successSpec();}
     */
    public static ResponseSpecification successSpec() {
        return new ResponseSpecBuilder()
            .expectStatusCode(200)
            .expectContentType(ContentType.JSON)
            .build();
    }
}
