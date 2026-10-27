package com.taskmanagement.adapter.ai;

/**
 * Exception thrown when communication with an AI model provider fails.
 */
public class AiCommunicationException extends RuntimeException {
    public AiCommunicationException(String message) {
        super(message);
    }

    public AiCommunicationException(String message, Throwable cause) {
        super(message, cause);
    }
}
