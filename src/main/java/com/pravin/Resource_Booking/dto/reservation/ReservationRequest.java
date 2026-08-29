package com.pravin.Resource_Booking.dto.reservation;

import com.pravin.Resource_Booking.entity.ReservationStatus;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReservationRequest(
        @NotNull(message = "Resource id is required")
        Long resourceId,

        @NotNull(message = "Start time is required")
        @Future(message = "Start time must be in the future")
        LocalDateTime startTime,

        @NotNull(message = "End time is required")
        LocalDateTime endTime,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.0", message = "Price must be zero or greater")
        BigDecimal price,

        ReservationStatus status
) {
    @AssertTrue(message = "End time must be after start time")
    public boolean isTimeRangeValid() {
        return startTime == null || endTime == null || endTime.isAfter(startTime);
    }
}
