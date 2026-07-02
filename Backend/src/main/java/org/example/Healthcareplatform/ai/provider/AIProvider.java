package org.example.Healthcareplatform.ai.provider;

/**
 * Abstraction for AI chat providers.
 * <p>
 * The provider knows <b>only</b> about prompts — never about application DTOs.
 * This makes swapping providers trivial.
 */
public interface AIProvider {

    /**
     * Send a prompt to the AI provider and return the plain-text response.
     *
     * @param prompt the complete prompt (system + user + history as one string)
     * @return the AI-generated text response
     */
    String chat(String prompt);

    /**
     * @return a human-readable name for this provider, e.g. "OpenRouter" or "Mock"
     */
    String providerName();

    /**
     * @return the model this provider is currently configured to use
     */
    String modelName();
}
