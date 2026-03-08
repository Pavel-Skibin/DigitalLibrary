package org.nahap.bookcatalogservice.exception;

public class CommentAlreadyExistsException extends RuntimeException {
    public CommentAlreadyExistsException(String message) {
        super(message);
    }
}