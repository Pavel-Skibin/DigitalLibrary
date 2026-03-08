package org.nahap.bookcatalogservice.exception;

public class UserAlreadyExistsException extends RuntimeException {
    public UserAlreadyExistsException(String string) {
        super(string);
    }
}
