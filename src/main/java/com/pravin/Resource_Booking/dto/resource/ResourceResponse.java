package com.pravin.Resource_Booking.dto.resource;

import com.pravin.Resource_Booking.entity.ResourceType;

import java.time.LocalDateTime;

public record ResourceResponse(
        Long id,
        String name,
        String description,
        ResourceType type,
        boolean available,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
