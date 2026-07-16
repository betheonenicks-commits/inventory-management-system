package com.iams.storage.infrastructure;

/**
 * The object store couldn't serve a request. Surfaces as a 503 via
 * ApiExceptionHandler - the client did nothing wrong and retrying later is
 * the correct response, so neither 400 nor 500 tells the truth.
 */
public class StorageUnavailableException extends RuntimeException {

    public StorageUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
