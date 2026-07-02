package org.example.Healthcareplatform.ai.exception;

/**
 * Thrown when the selected AI provider is unavailable or misconfigured.
 */
public class ProviderUnavailableException extends AIException {

    public ProviderUnavailableException(String providerName) {
        super("AI provider '" + providerName + "' is currently unavailable");
    }

    public ProviderUnavailableException(String providerName, Throwable cause) {
        super("AI provider '" + providerName + "' is currently unavailable", cause);
    }
}
