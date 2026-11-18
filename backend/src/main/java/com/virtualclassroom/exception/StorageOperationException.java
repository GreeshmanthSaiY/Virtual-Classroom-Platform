package com.virtualclassroom.exception;

/**
 * Thrown when an upstream cloud storage operation fails in a cloud-required
 * environment (staging/production) where silent local fallback is prohibited.
 */
public class StorageOperationException extends RuntimeException {

    public StorageOperationException(String message) {
        super(message);
    }

    public StorageOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
