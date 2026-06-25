package com.enterprise.framework.utils;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * JSON test data utility using Gson.
 *
 * <p>Provides a clean API for:
 * <ul>
 *   <li>Loading JSON test data files from the classpath</li>
 *   <li>Deserializing JSON to POJOs, Maps, or Lists</li>
 *   <li>Serializing objects to JSON (useful for API test body construction)</li>
 *   <li>Parsing individual JSON values from a larger object</li>
 * </ul>
 *
 * <p><b>File location convention:</b> JSON test data files live in
 * {@code src/test/resources/testdata/}. Pass the relative classpath path:
 * {@code "testdata/users.json"}
 */
public final class JsonUtils {

    private static final Logger log = LogManager.getLogger(JsonUtils.class);
    private static final Gson gson = new GsonBuilder()
        .setPrettyPrinting()
        .setFieldNamingPolicy(FieldNamingPolicy.IDENTITY)
        .create();

    private JsonUtils() { }

    // -------------------------------------------------------------------------
    // Deserialization
    // -------------------------------------------------------------------------

    /**
     * Reads a JSON file and deserializes it into the given type.
     *
     * @param classpathPath relative path from classpath root (e.g. "testdata/users.json")
     * @param type          target type (use {@code MyClass.class} or {@code new TypeToken<>(){}.getType()})
     * @return deserialized object
     */
    public static <T> T readAs(String classpathPath, Type type) {
        log.debug("Loading JSON: {}", classpathPath);
        try (InputStream is = getStream(classpathPath);
             Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            return gson.fromJson(reader, type);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read JSON file: " + classpathPath, e);
        }
    }

    /** Reads a JSON file into a list of maps — the most common format for test data tables. */
    public static List<Map<String, Object>> readAsListOfMaps(String classpathPath) {
        Type type = new TypeToken<List<Map<String, Object>>>(){}.getType();
        return readAs(classpathPath, type);
    }

    /** Reads a JSON file into a single map. */
    public static Map<String, Object> readAsMap(String classpathPath) {
        Type type = new TypeToken<Map<String, Object>>(){}.getType();
        return readAs(classpathPath, type);
    }

    /** Reads a JSON file into a POJO class. */
    public static <T> T readAs(String classpathPath, Class<T> clazz) {
        return readAs(classpathPath, (Type) clazz);
    }

    // -------------------------------------------------------------------------
    // Serialization
    // -------------------------------------------------------------------------

    public static String toJson(Object object) {
        return gson.toJson(object);
    }

    public static String toPrettyJson(Object object) {
        return gson.toJson(object);
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        return gson.fromJson(json, clazz);
    }

    public static <T> T fromJson(String json, Type type) {
        return gson.fromJson(json, type);
    }

    // -------------------------------------------------------------------------
    // JSON path-like navigation
    // -------------------------------------------------------------------------

    /**
     * Extracts a string value from a JSON file using dot-notation path.
     *
     * <p>Example: {@code JsonUtils.getValue("testdata/config.json", "database.host")}
     */
    public static String getValue(String classpathPath, String dotPath) {
        try (InputStream is = getStream(classpathPath);
             Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            JsonElement element = navigatePath(root, dotPath.split("\\."));
            return element != null ? element.getAsString() : null;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read JSON file: " + classpathPath, e);
        }
    }

    /** Parses a JSON string and extracts a value by dot-notation path. */
    public static String getValueFromString(String jsonString, String dotPath) {
        JsonObject root = JsonParser.parseString(jsonString).getAsJsonObject();
        JsonElement element = navigatePath(root, dotPath.split("\\."));
        return element != null ? element.getAsString() : null;
    }

    /**
     * Converts a list-of-maps JSON file to a TestNG-compatible 2D Object array.
     */
    public static Object[][] toTestNgDataProvider(String classpathPath) {
        List<Map<String, Object>> rows = readAsListOfMaps(classpathPath);
        Object[][] data = new Object[rows.size()][1];
        for (int i = 0; i < rows.size(); i++) {
            data[i][0] = rows.get(i);
        }
        return data;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private static InputStream getStream(String classpathPath) {
        InputStream is = JsonUtils.class.getClassLoader().getResourceAsStream(classpathPath);
        if (is == null) {
            throw new IllegalArgumentException("JSON file not found on classpath: " + classpathPath);
        }
        return is;
    }

    private static JsonElement navigatePath(JsonObject root, String[] keys) {
        JsonElement current = root;
        for (String key : keys) {
            if (current == null || !current.isJsonObject()) return null;
            current = current.getAsJsonObject().get(key);
        }
        return current;
    }
}
