package com.pravin.Resource_Booking.service;

import com.pravin.Resource_Booking.dto.auth.AuthResponse;
import com.pravin.Resource_Booking.dto.auth.LoginRequest;
import com.pravin.Resource_Booking.dto.auth.RegisterRequest;
import com.pravin.Resource_Booking.dto.user.UserResponse;
import com.pravin.Resource_Booking.entity.Role;
import com.pravin.Resource_Booking.entity.User;
import com.pravin.Resource_Booking.exception.BadRequestException;
import com.pravin.Resource_Booking.repository.UserRepository;
import com.pravin.Resource_Booking.security.CustomUserDetails;
import com.pravin.Resource_Booking.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       UserRepository userRepository,
                       PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();
        User user = userRepository.findByUsername(principal.getUsername())
                .orElseThrow(() -> new BadRequestException("User not found: " + principal.getUsername()));

        String token = jwtService.generateToken(principal);
        return new AuthResponse(
                token,
                "Bearer",
                jwtService.getExpiration(),
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getEmail(),
                user.getRole());
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String username = request.username().trim();
        String email = request.email().trim();

        if (userRepository.existsByUsername(username)) {
            throw new BadRequestException("Username '" + username + "' is already taken");
        }
        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("Email '" + email + "' is already registered");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setFullName(request.fullName().trim());
        user.setPassword(passwordEncoder.encode(request.password()));
        // Self-registration always creates a regular USER; ADMIN accounts are provisioned
        // by the seed data / administrator.
        user.setRole(Role.USER);

        user = userRepository.save(user);
        return toResponse(user);
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getEmail(),
                user.getFullName(), user.getRole(), user.getCreatedAt());
    }
}
