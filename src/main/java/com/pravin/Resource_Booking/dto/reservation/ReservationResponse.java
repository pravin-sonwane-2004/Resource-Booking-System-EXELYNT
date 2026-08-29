package com.pravin.Resource_Booking.dto.reservation;

import com.pravin.Resource_Booking.entity.ReservationStatus;
import com.pravin.Resource_Booking.entity.ResourceType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReservationResponse(
        Long id,
        Long userId,
        String username,
        Long resourceId,
        String resourceName,
        ResourceType resourceType,
        LocalDateTime startTime,
        LocalDateTime endTime,
        ReservationStatus status,
        BigDecimal price,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
