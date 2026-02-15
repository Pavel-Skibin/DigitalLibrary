package org.nahap.bookcatalogservice.exception;

public class CommentNotFoundException extends RuntimeException {
    public CommentNotFoundException(String message) {
        super(message);
    }
}