package com.pravin.Resource_Booking.exception;

/**
 * Thrown when an operation conflicts with existing data, e.g. a resource that is
 * already booked during the requested time window (maps to HTTP 409).
 */
public class ReservationConflictException extends RuntimeException {

    public ReservationConflictException(String message) {
        super(message);
    }
}
