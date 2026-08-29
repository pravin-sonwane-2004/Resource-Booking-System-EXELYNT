package com.pravin.Resource_Booking.dto.reservation;

import com.pravin.Resource_Booking.entity.ReservationStatus;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Partial update payload for a reservation. All fields are optional; only the
 * non-null ones are applied by the service (with role-based restrictions).
 */
public record ReservationUpdateRequest(
        LocalDateTime startTime,
        LocalDateTime endTime,
        @DecimalMin(value = "0.0", message = "Price must be zero or greater")
        BigDecimal price,
        ReservationStatus status
) {
}
