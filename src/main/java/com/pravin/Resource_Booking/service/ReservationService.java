package com.pravin.Resource_Booking.service;

import com.pravin.Resource_Booking.dto.reservation.ReservationRequest;
import com.pravin.Resource_Booking.dto.reservation.ReservationResponse;
import com.pravin.Resource_Booking.dto.reservation.ReservationUpdateRequest;
import com.pravin.Resource_Booking.entity.Reservation;
import com.pravin.Resource_Booking.entity.ReservationStatus;
import com.pravin.Resource_Booking.entity.Resource;
import com.pravin.Resource_Booking.entity.User;
import com.pravin.Resource_Booking.exception.BadRequestException;
import com.pravin.Resource_Booking.exception.ReservationConflictException;
import com.pravin.Resource_Booking.exception.ResourceNotFoundException;
import com.pravin.Resource_Booking.repository.ReservationRepository;
import com.pravin.Resource_Booking.repository.ResourceRepository;
import com.pravin.Resource_Booking.repository.UserRepository;
import com.pravin.Resource_Booking.security.CustomUserDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * The acting user is always derived from the JWT principal in
 * {@link SecurityContextHolder} - never from the request body. Object-level
 * authorization is enforced here: an ADMIN sees/manages all reservations, while
 * a regular USER only sees/manages their own.
 */
@Service
@Transactional
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ResourceRepository resourceRepository;
    private final UserRepository userRepository;

    public ReservationService(ReservationRepository reservationRepository,
                              ResourceRepository resourceRepository,
                              UserRepository userRepository) {
        this.reservationRepository = reservationRepository;
        this.resourceRepository = resourceRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Page<ReservationResponse> findAll(String status, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        CustomUserDetails user = currentUser();
        Long userId = user.isAdmin() ? null : user.getId();
        ReservationStatus reservationStatus = parseStatus(status);
        return reservationRepository
                .findFiltered(userId, reservationStatus, minPrice, maxPrice, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ReservationResponse getById(Long id) {
        CustomUserDetails user = currentUser();
        Reservation reservation = find(id);
        if (!user.isAdmin() && !reservation.getUser().getId().equals(user.getId())) {
            // 404 (not 403) so we do not leak the existence of others' reservations.
            throw new ResourceNotFoundException("Reservation not found: " + id);
        }
        return toResponse(reservation);
    }

    public ReservationResponse create(ReservationRequest request) {
        CustomUserDetails user = currentUser();
        User owner = userRepository.findById(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + user.getId()));
        Resource resource = resourceRepository.findById(request.resourceId())
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found: " + request.resourceId()));

        if (!resource.isAvailable()) {
            throw new BadRequestException("Resource '" + resource.getName() + "' is not available for booking");
        }
        if (!request.isTimeRangeValid()) {
            throw new BadRequestException("End time must be after start time");
        }

        List<Reservation> overlapping = reservationRepository.findOverlapping(
                resource.getId(), request.startTime(), request.endTime(), ReservationStatus.CANCELLED);
        if (!overlapping.isEmpty()) {
            throw new ReservationConflictException("Resource is already booked during the requested time window");
        }

        Reservation reservation = new Reservation();
        reservation.setUser(owner);
        reservation.setResource(resource);
        reservation.setStartTime(request.startTime());
        reservation.setEndTime(request.endTime());
        reservation.setPrice(request.price());
        reservation.setStatus(request.status() == null ? ReservationStatus.PENDING : request.status());

        return toResponse(reservationRepository.save(reservation));
    }

    public ReservationResponse update(Long id, ReservationUpdateRequest request) {
        CustomUserDetails user = currentUser();
        Reservation reservation = find(id);
        boolean isOwner = reservation.getUser().getId().equals(user.getId());

        if (!user.isAdmin() && !isOwner) {
            throw new ResourceNotFoundException("Reservation not found: " + id);
        }

        if (!user.isAdmin()) {
            // A regular USER may only cancel their own reservation.
            if (request.status() == null || request.status() != ReservationStatus.CANCELLED) {
                throw new AccessDeniedException("USER role can only cancel their own reservation");
            }
            if (reservation.getStatus() == ReservationStatus.CANCELLED) {
                throw new BadRequestException("Reservation is already cancelled");
            }
            reservation.setStatus(ReservationStatus.CANCELLED);
            return toResponse(reservationRepository.save(reservation));
        }

        // ADMIN can update status, price and the time window.
        if (request.status() != null) {
            reservation.setStatus(request.status());
        }
        if (request.price() != null) {
            reservation.setPrice(request.price());
        }
        LocalDateTime newStart = request.startTime() != null ? request.startTime() : reservation.getStartTime();
        LocalDateTime newEnd = request.endTime() != null ? request.endTime() : reservation.getEndTime();
        if (!newEnd.isAfter(newStart)) {
            throw new BadRequestException("End time must be after start time");
        }
        reservation.setStartTime(newStart);
        reservation.setEndTime(newEnd);

        return toResponse(reservationRepository.save(reservation));
    }

    public void delete(Long id) {
        CustomUserDetails user = currentUser();
        Reservation reservation = find(id);
        if (!user.isAdmin() && !reservation.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Reservation not found: " + id);
        }
        reservationRepository.delete(reservation);
    }

    private Reservation find(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found: " + id));
    }

    private ReservationStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return ReservationStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid reservation status: " + status);
        }
    }

    private CustomUserDetails currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails principal)) {
            throw new AccessDeniedException("Authentication required");
        }
        return principal;
    }

    private ReservationResponse toResponse(Reservation reservation) {
        Resource resource = reservation.getResource();
        return new ReservationResponse(
                reservation.getId(),
                reservation.getUser().getId(),
                reservation.getUser().getUsername(),
                resource.getId(),
                resource.getName(),
                resource.getType(),
                reservation.getStartTime(),
                reservation.getEndTime(),
                reservation.getStatus(),
                reservation.getPrice(),
                reservation.getCreatedAt(),
                reservation.getUpdatedAt());
    }
}
