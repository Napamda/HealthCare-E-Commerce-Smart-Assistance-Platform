package org.example.Healthcareplatform.ai.provider;

public interface AIProvider {

    String chat(String prompt);

    String providerName();

    String modelName();
}
