package org.example.Healthcareplatform;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

public record ApiCase(
        String id,
        String folder,
        String method,
        String path,
        String auth,
        JsonNode body,
        List<Integer> expected_statuses) {
}
