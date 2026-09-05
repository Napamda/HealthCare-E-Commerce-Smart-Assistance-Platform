package org.example.Healthcareplatform;

import com.fasterxml.jackson.databind.JsonNode;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ApiContractTest {
    private static final Pattern VARIABLE = Pattern.compile("\\{\\{([A-Za-z][A-Za-z0-9]*)}}" );
    private static final List<ApiCase> CASES = TestCaseCatalog.load();
    private static final long MAX_RESPONSE_MS = Long.parseLong(System.getenv().getOrDefault("MAX_RESPONSE_TIME_MS", "3000"));
    private static final Map<String, String> TOKEN_VARIABLES = Map.of(
            "user", "USER_TOKEN", "staff", "STAFF_TOKEN", "admin", "ADMIN_TOKEN");

    @BeforeAll
    static void configureClient() {
        RestAssured.baseURI = System.getenv().getOrDefault("BASE_URL", "http://localhost:8080");
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @TestFactory
    Stream<DynamicTest> branchDerivedApiContracts() {
        return CASES.stream().map(apiCase -> DynamicTest.dynamicTest(
                apiCase.folder() + " :: " + apiCase.id(),
                () -> assertContract(apiCase)));
    }

    @TestFactory
    Stream<DynamicTest> protectedRoutesRejectAnonymousRequests() {
        return CASES.stream().filter(apiCase -> !"none".equals(apiCase.auth())).map(apiCase ->
                DynamicTest.dynamicTest("anonymous denied :: " + apiCase.id(), () -> {
                    String path = resolve(apiCase.path());
                    assumeTrue(!path.contains("{{"), "Fixture variables are not configured");
                    Response response = request(apiCase, null).request(apiCase.method(), path);
                    assertTrue(response.statusCode() == 401 || response.statusCode() == 403,
                            () -> "Protected route accepted anonymous request: " + response.statusCode() + " " + response.asString());
                }));
    }

    @Test
    void malformedBearerTokenIsRejected() {
        Response response = RestAssured.given().header("Authorization", "Bearer not-a-jwt")
                .get("/api/users/me");
        assertTrue(response.statusCode() == 401 || response.statusCode() == 403);
    }

    @Test
    void catalogContainsAllBranchDerivedCases() {
        assertEquals(110, CASES.size(), "The reviewed cross-branch catalog must not silently lose endpoints");
        assertEquals(110, CASES.stream().map(ApiCase::id).distinct().count(), "Case IDs must be unique");
    }

    private static void assertContract(ApiCase apiCase) {
        String path = resolve(apiCase.path());
        assumeTrue(!path.contains("{{"), "Fixture variables required for " + path);
        String token = tokenFor(apiCase.auth()).orElse(null);
        if (!"none".equals(apiCase.auth())) {
            assumeTrue(token != null && !token.isBlank(), "Set " + TOKEN_VARIABLES.get(apiCase.auth()));
        }
        Response response = assertTimeoutPreemptively(Duration.ofSeconds(12),
                () -> request(apiCase, token).request(apiCase.method(), path));
        assertTrue(response.statusCode() < 500, () -> "Server error: " + response.asString());
        assertTrue(apiCase.expected_statuses().contains(response.statusCode()),
                () -> "Expected " + apiCase.expected_statuses() + " but received " + response.statusCode() + ": " + response.asString());
        assertTrue(response.time() < MAX_RESPONSE_MS,
                () -> "Response exceeded " + MAX_RESPONSE_MS + "ms: " + response.time() + "ms");
        if (response.statusCode() != 204 && !response.asString().isBlank()) {
            assertTrue(response.contentType().toLowerCase(Locale.ROOT).contains("json"), "Response must be JSON");
            assertDoesNotThrow(response::jsonPath, "Response body must be valid JSON");
        }
    }

    private static RequestSpecification request(ApiCase apiCase, String token) {
        RequestSpecification request = RestAssured.given().accept("application/json");
        if (token != null) request.auth().oauth2(token);
        JsonNode body = apiCase.body();
        if (body != null && !body.isNull()) request.contentType("application/json").body(resolve(body.toString()));
        return request;
    }

    private static Optional<String> tokenFor(String role) {
        if ("none".equals(role)) return Optional.empty();
        return Optional.ofNullable(System.getenv(TOKEN_VARIABLES.get(role)));
    }

    private static String resolve(String source) {
        Matcher matcher = VARIABLE.matcher(source);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String env = matcher.group(1).replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase(Locale.ROOT);
            matcher.appendReplacement(result, Matcher.quoteReplacement(System.getenv().getOrDefault(env, matcher.group())));
        }
        return matcher.appendTail(result).toString();
    }
}
