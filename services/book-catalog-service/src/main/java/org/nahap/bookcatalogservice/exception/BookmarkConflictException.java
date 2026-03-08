package org.nahap.bookcatalogservice.exception;

public class BookmarkConflictException extends RuntimeException {
    public BookmarkConflictException(String message) {
        super(message);
    }
}