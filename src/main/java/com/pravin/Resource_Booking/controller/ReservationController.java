package com.pravin.Resource_Booking.controller;

import com.pravin.Resource_Booking.dto.reservation.ReservationRequest;
import com.pravin.Resource_Booking.dto.reservation.ReservationResponse;
import com.pravin.Resource_Booking.dto.reservation.ReservationUpdateRequest;
import com.pravin.Resource_Booking.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/reservations")
@Tag(name = "Reservations", description = "Reservation management with filtering, pagination and sorting")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @Operation(summary = "List reservations (ADMIN: all, USER: own only) with optional filtering, pagination and sorting",
            description = "Query parameters: status (PENDING/CONFIRMED/CANCELLED), minPrice, maxPrice, "
                    + "page, size and sort (e.g. sort=createdAt,desc)")
    @GetMapping
    public ResponseEntity<Page<ReservationResponse>> getAll(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(reservationService.findAll(status, minPrice, maxPrice, pageable));
    }

    @Operation(summary = "Get a single reservation (USER: own only)")
    @GetMapping("/{id}")
    public ResponseEntity<ReservationResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(reservationService.getById(id));
    }

    @Operation(summary = "Create a reservation (owner is taken from the JWT)")
    @PostMapping
    public ResponseEntity<ReservationResponse> create(@Valid @RequestBody ReservationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reservationService.create(request));
    }

    @Operation(summary = "Update/cancel a reservation (USER: cancel own only; ADMIN: full update)")
    @PatchMapping("/{id}")
    public ResponseEntity<ReservationResponse> update(@PathVariable Long id,
                                                      @Valid @RequestBody ReservationUpdateRequest request) {
        return ResponseEntity.ok(reservationService.update(id, request));
    }

    @Operation(summary = "Delete a reservation (USER: own only)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        reservationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}