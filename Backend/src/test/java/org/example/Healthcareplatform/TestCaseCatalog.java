package org.example.Healthcareplatform;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

final class TestCaseCatalog {
    private static final ObjectMapper JSON = new ObjectMapper();

    private TestCaseCatalog() {}

    static List<ApiCase> load() {
        try (InputStream stream = TestCaseCatalog.class.getResourceAsStream("/test_cases.json")) {
            if (stream == null) throw new IllegalStateException("test_cases.json is missing from test resources");
            Map<String, JsonNode> root = JSON.readValue(stream, new TypeReference<>() {});
            return JSON.convertValue(root.get("cases"), new TypeReference<List<ApiCase>>() {});
        } catch (IOException error) {
            throw new IllegalStateException("Unable to load Java API case catalog", error);
        }
    }
}
