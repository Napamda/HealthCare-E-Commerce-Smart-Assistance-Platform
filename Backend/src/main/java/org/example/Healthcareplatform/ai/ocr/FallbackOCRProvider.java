package org.example.Healthcareplatform.ai.ocr;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FallbackOCRProvider implements OCRProvider {

    @Override
    public String extractText(byte[] imageBytes, String mimeType) {
        log.info("FallbackOCRProvider — returning placeholder for {} image ({} bytes)", mimeType, imageBytes.length);

        if (mimeType != null && mimeType.equals("application/pdf")) {
            return "[OCR unavailable for PDF — text extraction requires a PDF parsing library. "
                    + "Please manually enter the prescription text below.]";
        }

        return "[Automatic OCR was unable to extract text from this image. "
                + "Please type or paste the prescription text below.]";
    }

    @Override
    public String providerName() {
        return "Fallback";
    }
}
