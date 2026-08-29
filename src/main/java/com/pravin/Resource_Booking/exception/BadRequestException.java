package com.pravin.Resource_Booking.exception;

/**
 * Thrown for malformed or invalid requests (maps to HTTP 400).
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
