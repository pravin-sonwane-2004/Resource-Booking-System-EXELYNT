package com.pravin.Resource_Booking.dto.user;

import com.pravin.Resource_Booking.entity.Role;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String username,
        String email,
        String fullName,
        Role role,
        LocalDateTime createdAt
) {
}
