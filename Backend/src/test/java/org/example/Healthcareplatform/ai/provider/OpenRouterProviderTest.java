package org.example.Healthcareplatform.ai.provider;

import org.example.Healthcareplatform.ai.exception.ProviderUnavailableException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OpenRouterProviderTest {

    private static final String SUCCESS_BODY =
            "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"ok\"}}]}";

    @Test
    void retriesOnRetryableServerErrors() {
        QueuedResponseFactory factory = new QueuedResponseFactory();
        factory.queue(HttpStatus.INTERNAL_SERVER_ERROR, "boom");
        factory.queue(HttpStatus.INTERNAL_SERVER_ERROR, "boom");
        factory.queue(HttpStatus.OK, SUCCESS_BODY);

        OpenRouterProvider provider = new OpenRouterProvider(
                "https://example.com", "test-key", "test-model", factory);

        String result = provider.chat("prompt");

        assertEquals("ok", result);
        assertEquals(3, factory.executedCount());
    }

    @Test
    void doesNotRetryOnClientError() {
        QueuedResponseFactory factory = new QueuedResponseFactory();
        factory.queue(HttpStatus.BAD_REQUEST, "bad request");

        OpenRouterProvider provider = new OpenRouterProvider(
                "https://example.com", "test-key", "test-model", factory);

        assertThrows(ProviderUnavailableException.class, () -> provider.chat("prompt"));

        assertEquals(1, factory.executedCount());
    }

    private static class QueuedResponseFactory implements ClientHttpRequestFactory {

        private final Deque<ClientHttpResponse> responses = new ArrayDeque<>();
        private int executedCount;

        void queue(HttpStatusCode status, String body) {
            MockClientHttpResponse response = new MockClientHttpResponse(
                    body.getBytes(StandardCharsets.UTF_8), status);
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            responses.add(response);
        }

        @Override
        public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) {
            executedCount++;
            MockClientHttpRequest request = new MockClientHttpRequest(httpMethod, uri);
            request.setResponse(responses.poll());
            return request;
        }

        int executedCount() {
            return executedCount;
        }
    }
}
