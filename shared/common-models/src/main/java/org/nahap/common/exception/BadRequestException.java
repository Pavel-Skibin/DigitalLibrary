package org.nahap.common.exception;

/**
 * Исключение для некорректных запросов
 */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
