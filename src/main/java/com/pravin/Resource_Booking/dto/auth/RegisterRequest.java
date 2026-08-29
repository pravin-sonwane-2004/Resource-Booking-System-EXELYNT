package com.pravin.Resource_Booking.dto.auth;

import com.pravin.Resource_Booking.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 60, message = "Username must be between 3 and 60 characters")
        String username,

        @NotBlank(message = "Email is required")
        @Email(message = "A valid email is required")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 6, max = 120, message = "Password must be at least 6 characters")
        String password,

        @NotBlank(message = "Full name is required")
        @Size(min = 2, max = 120, message = "Full name must be between 2 and 120 characters")
        String fullName,

        Role role
) {
}
