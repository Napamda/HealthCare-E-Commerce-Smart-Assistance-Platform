package org.example.Healthcareplatform.ai.ocr;

public interface OCRProvider {

    String extractText(byte[] imageBytes, String mimeType);

    String providerName();
}
