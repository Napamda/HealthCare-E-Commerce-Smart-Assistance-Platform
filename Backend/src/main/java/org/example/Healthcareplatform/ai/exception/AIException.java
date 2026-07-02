package org.example.Healthcareplatform.ai.exception;

/**
 * Base exception for AI module errors.
 */
public class AIException extends RuntimeException {

    public AIException(String message) {
        super(message);
    }

    public AIException(String message, Throwable cause) {
        super(message, cause);
    }
}
