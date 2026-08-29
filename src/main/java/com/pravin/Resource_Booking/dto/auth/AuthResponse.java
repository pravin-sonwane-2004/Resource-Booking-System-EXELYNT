package com.pravin.Resource_Booking.dto.auth;

import com.pravin.Resource_Booking.entity.Role;

public record AuthResponse(
        String token,
        String tokenType,
        long expiresIn,
        Long id,
        String username,
        String fullName,
        String email,
        Role role
) {
}
