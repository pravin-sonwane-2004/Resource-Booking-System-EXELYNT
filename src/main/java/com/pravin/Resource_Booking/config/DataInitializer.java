package com.pravin.Resource_Booking.config;

import com.pravin.Resource_Booking.entity.Reservation;
import com.pravin.Resource_Booking.entity.ReservationStatus;
import com.pravin.Resource_Booking.entity.Resource;
import com.pravin.Resource_Booking.entity.ResourceType;
import com.pravin.Resource_Booking.entity.Role;
import com.pravin.Resource_Booking.entity.User;
import com.pravin.Resource_Booking.repository.ReservationRepository;
import com.pravin.Resource_Booking.repository.ResourceRepository;
import com.pravin.Resource_Booking.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Seeds the database with demo ADMIN/USER accounts and sample data so the API can
 * be exercised immediately. Disabled under the "test" profile.
 */
@Component
@Profile("!test")
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final ResourceRepository resourceRepository;
    private final ReservationRepository reservationRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           ResourceRepository resourceRepository,
                           ReservationRepository reservationRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.resourceRepository = resourceRepository;
        this.reservationRepository = reservationRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        seedUsers();
        seedResourcesAndReservations();
        log.info("Seed data initialised.");
    }

    private void seedUsers() {
        seedUser("admin", "admin@booking.com", "admin123", "System Administrator", Role.ADMIN);
        seedUser("user", "user@booking.com", "user123", "Demo User", Role.USER);
        seedUser("jane", "jane@booking.com", "user123", "Jane Doe", Role.USER);
    }

    private void seedResourcesAndReservations() {
        Resource boardRoom = seedResource("Board Room A", "Large meeting room with projector and whiteboard", ResourceType.ROOM);
        Resource sedan = seedResource("Toyota Corolla", "Hybrid sedan for site visits", ResourceType.VEHICLE);
        seedResource("Projector XG-300", "Portable HD projector", ResourceType.EQUIPMENT);

        User user = userRepository.findByUsername("user").orElseThrow();
        User admin = userRepository.findByUsername("admin").orElseThrow();

        if (reservationRepository.count() == 0) {
            seedReservation(user, boardRoom,
                    LocalDateTime.now().plusDays(1).withHour(9).withMinute(0),
                    LocalDateTime.now().plusDays(1).withHour(11).withMinute(0),
                    ReservationStatus.CONFIRMED, new BigDecimal("150.00"));
            seedReservation(admin, sedan,
                    LocalDateTime.now().plusDays(2).withHour(14).withMinute(0),
                    LocalDateTime.now().plusDays(2).withHour(16).withMinute(0),
                    ReservationStatus.PENDING, new BigDecimal("80.50"));
        }
    }

    private User seedUser(String username, String email, String rawPassword, String fullName, Role role) {
        if (userRepository.existsByUsername(username)) {
            return userRepository.findByUsername(username).orElseThrow();
        }
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setFullName(fullName);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        log.info("Seeded {} user '{}'", role, username);
        return userRepository.save(user);
    }

    private Resource seedResource(String name, String description, ResourceType type) {
        if (resourceRepository.findAll().stream().anyMatch(r -> r.getName().equals(name))) {
            return resourceRepository.findAll().stream()
                    .filter(r -> r.getName().equals(name))
                    .findFirst().orElseThrow();
        }
        Resource resource = new Resource();
        resource.setName(name);
        resource.setDescription(description);
        resource.setType(type);
        resource.setAvailable(true);
        log.info("Seeded resource '{}'", name);
        return resourceRepository.save(resource);
    }

    private void seedReservation(User user, Resource resource, LocalDateTime start, LocalDateTime end,
                                 ReservationStatus status, BigDecimal price) {
        Reservation reservation = new Reservation();
        reservation.setUser(user);
        reservation.setResource(resource);
        reservation.setStartTime(start);
        reservation.setEndTime(end);
        reservation.setStatus(status);
        reservation.setPrice(price);
        reservationRepository.save(reservation);
        log.info("Seeded reservation for '{}' on '{}'", user.getUsername(), resource.getName());
    }
}