package org.example;

import org.json.JSONArray;
import org.json.JSONObject;
import org.openqa.selenium.By;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class LocatorHelper {

    private final Map<String, By> locatorMap = new HashMap<>();

    public LocatorHelper() {
        JSONArray elements = loadElementsJson();
        for (int i = 0; i < elements.length(); i++) {
            JSONObject element = elements.getJSONObject(i);

            String key   = element.optString("key", null);
            String type  = element.optString("type", null);
            String value = element.optString("value", null);

            if (key == null || type == null || value == null) {
                throw new RuntimeException("elements.json: key/type/value alanlarından biri eksik! Index=" + i);
            }

            locatorMap.put(key, buildBy(type, value));
        }
    }

    private JSONArray loadElementsJson() {
        try (InputStream inputStream =
                     getClass().getClassLoader().getResourceAsStream("elements/elements.json")) {

            if (inputStream == null) {
                throw new RuntimeException("elements.json dosyası CLASSPATH içinde bulunamadı! (resources/elements/elements.json?)");
            }

            try (Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8).useDelimiter("\\A")) {
                String jsonContent = scanner.hasNext() ? scanner.next() : "[]";
                return new JSONArray(jsonContent);
            }
        } catch (Exception e) {
            throw new RuntimeException("elements.json okunurken hata oluştu!", e);
        }
    }

    private By buildBy(String rawType, String value) {
        String type = rawType.trim().toLowerCase(Locale.ROOT);
        switch (type) {
            case "css":
            case "cssselector":
                return By.cssSelector(value);
            case "id":
                return By.id(value);
            case "xpath":
                return By.xpath(value);
            case "linktext":
                return By.linkText(value);
            case "partiallinktext":
                return By.partialLinkText(value);
            case "name":
                return By.name(value);
            case "classname":
            case "class":
                return By.className(value);
            case "tag":
            case "tagname":
                return By.tagName(value);
            default:
                throw new RuntimeException("Desteklenmeyen locator tipi: " + rawType + " (value=" + value + ")");
        }
    }

    /** Anahtar yoksa RuntimeException atar. */
    public By getLocator(String key) {
        By by = locatorMap.get(key);
        if (by == null) {
            throw new RuntimeException("elements.json içinde '" + key + "' anahtarı bulunamadı! Mevcut anahtarlar: " + locatorMap.keySet());
        }
        return by;
    }

    /** Debug için: tüm anahtarları döndür. */
    public Set<String> keys() {
        return Collections.unmodifiableSet(locatorMap.keySet());
    }
}
