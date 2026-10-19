package support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.openqa.selenium.By;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads Selenium locators from classpath JSON catalogs.
 * Supports single locator definitions and ordered fallback arrays.
 */
public final class LocatorReader {

    private static final String BASE_PATH = "element-infos/";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Map<String, JsonNode> CACHE = new ConcurrentHashMap<>();

    private LocatorReader() {
    }

    public static By getLocator(String file, String key) {
        JsonNode entry = getEntry(file, key);
        if (!entry.isObject()) {
            throw new IllegalArgumentException(
                    "Expected a single locator object: " + file + " -> " + key);
        }
        return toBy(entry, file, key);
    }

    public static By[] getLocators(String file, String key) {
        JsonNode entries = getEntry(file, key);
        if (!entries.isArray() || entries.isEmpty()) {
            throw new IllegalArgumentException(
                    "Expected a nonempty locator array: " + file + " -> " + key);
        }
        By[] result = new By[entries.size()];
        for (int i = 0; i < entries.size(); i++) {
            result[i] = toBy(entries.get(i), file, key + "[" + i + "]");
        }
        return result;
    }

    private static JsonNode getEntry(String file, String key) {
        JsonNode entry = CACHE.computeIfAbsent(file, LocatorReader::loadFile).get(key);
        if (entry == null || entry.isNull()) {
            throw new IllegalArgumentException(
                    "Locator key not found: " + file + " -> " + key);
        }
        return entry;
    }

    private static JsonNode loadFile(String file) {
        if (file == null || file.isBlank() || file.startsWith("/")
                || file.contains("..") || file.contains("\\\\")) {
            throw new IllegalArgumentException("Invalid locator file path: " + file);
        }

        String path = BASE_PATH + file;
        try (InputStream stream = LocatorReader.class.getClassLoader()
                .getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException("Locator catalog not found: " + path);
            }
            JsonNode document = MAPPER.readTree(stream);
            if (!document.isObject()) {
                throw new IllegalStateException("Locator catalog must be a JSON object: " + path);
            }
            return document;
        } catch (IOException e) {
            throw new IllegalStateException("Could not read locator catalog: " + path, e);
        }
    }

    private static By toBy(JsonNode entry, String file, String key) {
        if (entry == null || !entry.isObject()) {
            throw new IllegalArgumentException("Invalid locator: " + file + " -> " + key);
        }
        JsonNode typeNode = entry.get("type");
        JsonNode valueNode = entry.get("value");
        if (typeNode == null || !typeNode.isTextual()
                || valueNode == null || !valueNode.isTextual()
                || valueNode.asText().isBlank()) {
            throw new IllegalArgumentException(
                    "Locator needs nonempty type and value: " + file + " -> " + key);
        }

        String value = valueNode.asText();
        switch (typeNode.asText()) {
            case "css":
            case "cssSelector":
                return By.cssSelector(value);
            case "xpath":
                return By.xpath(value);
            case "id":
                return By.id(value);
            case "name":
                return By.name(value);
            case "className":
                return By.className(value);
            case "tagName":
                return By.tagName(value);
            case "linkText":
                return By.linkText(value);
            case "partialLinkText":
                return By.partialLinkText(value);
            default:
                throw new IllegalArgumentException(
                        "Unsupported locator type: " + typeNode.asText()
                                + " in " + file + " -> " + key);
        }
    }
}
