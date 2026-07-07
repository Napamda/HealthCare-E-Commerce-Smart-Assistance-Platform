package org.example.Healthcareplatform.ai.ocr;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class OCRService {

    private final OCRProvider primaryProvider;

    public OCRService(OCRProvider primaryProvider) {
        this.primaryProvider = primaryProvider;
        log.info("OCRService initialized — provider={}", primaryProvider.providerName());
    }

    public String extractText(byte[] imageBytes, String mimeType) {
        try {
            String result = primaryProvider.extractText(imageBytes, mimeType);
            if (result != null && !result.isBlank()
                    && !result.startsWith("[") && !result.contains("No readable text detected")) {
                log.info("OCR succeeded — {} chars", result.length());
                return result;
            }
            log.warn("Primary OCR returned empty/placeholder result, using fallback");
        } catch (Exception e) {
            log.error("Primary OCR failed: {}", e.getMessage());
        }

        FallbackOCRProvider fallback = new FallbackOCRProvider();
        return fallback.extractText(imageBytes, mimeType);
    }

    public String providerName() {
        return primaryProvider.providerName();
    }
}
