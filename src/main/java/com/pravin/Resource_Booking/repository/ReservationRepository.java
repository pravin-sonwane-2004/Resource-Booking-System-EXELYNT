package com.pravin.Resource_Booking.repository;

import com.pravin.Resource_Booking.entity.Reservation;
import com.pravin.Resource_Booking.entity.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    /**
     * Finds reservations applying optional filters.
     * <p>
     * When {@code userId} is provided the query is restricted to that single user
     * (used for USER role, ownership enforced by the service layer). When it is
     * {@code null} all reservations are returned (used for ADMIN role).
     * {@code status}, {@code minPrice} and {@code maxPrice} are optional filters.
     * Pagination and sorting are applied through the {@link Pageable} argument.
     */
    @Query("""
            SELECT r FROM Reservation r
            WHERE (:userId IS NULL OR r.user.id = :userId)
              AND (:status IS NULL OR r.status = :status)
              AND (:minPrice IS NULL OR r.price >= :minPrice)
              AND (:maxPrice IS NULL OR r.price <= :maxPrice)
            """)
    Page<Reservation> findFiltered(@Param("userId") Long userId,
                                   @Param("status") ReservationStatus status,
                                   @Param("minPrice") BigDecimal minPrice,
                                   @Param("maxPrice") BigDecimal maxPrice,
                                   Pageable pageable);

    Page<Reservation> findByUserId(Long userId, Pageable pageable);

    Optional<Reservation> findByIdAndUserId(Long id, Long userId);

    /**
     * Finds reservations for a resource whose time range overlaps the given range and
     * that have not been cancelled (an overlap with a cancelled booking is irrelevant).
     */
    @Query("""
            SELECT r FROM Reservation r
            WHERE r.resource.id = :resourceId
              AND r.status <> :cancelled
              AND r.startTime < :end
              AND r.endTime > :start
            """)
    List<Reservation> findOverlapping(@Param("resourceId") Long resourceId,
                                      @Param("start") LocalDateTime start,
                                      @Param("end") LocalDateTime end,
                                      @Param("cancelled") ReservationStatus cancelled);
}
