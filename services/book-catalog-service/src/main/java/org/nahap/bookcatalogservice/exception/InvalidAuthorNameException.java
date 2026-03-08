package org.nahap.bookcatalogservice.exception;

public class InvalidAuthorNameException extends RuntimeException {
    public InvalidAuthorNameException(String message) {
        super(message);
    }
}