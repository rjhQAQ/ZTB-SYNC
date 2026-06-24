package org.example.ztbsync.exception;

public class RagSearchUnavailableException extends RuntimeException {

    public RagSearchUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public RagSearchUnavailableException(String message) {
        super(message);
    }
}
