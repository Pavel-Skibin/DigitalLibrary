package org.nahap.bookcatalogservice.exception;

public class GenreHasBooksException extends RuntimeException {
    public GenreHasBooksException(String message) {
        super(message);
    }
}