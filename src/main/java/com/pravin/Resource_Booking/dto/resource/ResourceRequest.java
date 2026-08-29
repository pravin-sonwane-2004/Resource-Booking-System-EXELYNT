package com.pravin.Resource_Booking.dto.resource;

import com.pravin.Resource_Booking.entity.ResourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ResourceRequest(
        @NotBlank(message = "Resource name is required")
        @Size(max = 120, message = "Resource name must be at most 120 characters")
        String name,

        @Size(max = 500, message = "Description must be at most 500 characters")
        String description,

        @NotNull(message = "Resource type is required")
        ResourceType type,

        @NotNull(message = "Availability flag is required")
        Boolean available
) {
}
