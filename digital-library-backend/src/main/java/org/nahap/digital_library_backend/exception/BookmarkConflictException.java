package org.nahap.digital_library_backend.exception;

public class BookmarkConflictException extends RuntimeException {
    public BookmarkConflictException(String message) {
        super(message);
    }
}