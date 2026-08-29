package com.pravin.Resource_Booking;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pravin.Resource_Booking.entity.Role;
import com.pravin.Resource_Booking.entity.User;
import com.pravin.Resource_Booking.repository.ReservationRepository;
import com.pravin.Resource_Booking.repository.ResourceRepository;
import com.pravin.Resource_Booking.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end integration tests (H2 + MockMvc + real security filter chain)
 * covering authentication, RBAC, reservation ownership, filtering, pagination
 * and sorting.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @BeforeEach
    void setUp() {
        reservationRepository.deleteAll();
        resourceRepository.deleteAll();
        userRepository.deleteAll();
        seedUser("admin", "admin@booking.com", "admin123", "Admin User", Role.ADMIN);
        seedUser("alice", "alice@booking.com", "user123", "Alice", Role.USER);
        seedUser("bob", "bob@booking.com", "user123", "Bob", Role.USER);
    }

    // ------------------------------------------------------------------ Auth

    @Test
    void loginWithValidCredentials_returnsToken() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"password\":\"user123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void loginWithInvalidCredentials_returns401() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginWithMissingFields_returns400() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\"}"))
                .andExpect(status().isBadRequest());
    }

    // ----------------------------------------------------------- Security

    @Test
    void protectedEndpointWithoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/resources")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/reservations")).andExpect(status().isUnauthorized());
    }

    @Test
    void userCannotCreateResource_returns403() throws Exception {
        String token = login("alice", "user123");
        mockMvc.perform(post("/api/resources")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Room X\",\"type\":\"ROOM\",\"available\":true}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanCreateAndReadResources() throws Exception {
        String token = login("admin", "admin123");
        long id = createResource(token, "Board Room A", "ROOM");
        mockMvc.perform(get("/api/resources").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id));
        mockMvc.perform(get("/api/resources/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Board Room A"));
    }

    @Test
    void userCanReadResourcesButCannotModifyThem() throws Exception {
        String adminToken = login("admin", "admin123");
        String userToken = login("alice", "user123");
        long id = createResource(adminToken, "Projector", "EQUIPMENT");

        mockMvc.perform(get("/api/resources/" + id).header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/resources/" + id)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Hacked\",\"type\":\"EQUIPMENT\",\"available\":true}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/resources/" + id)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------ Reservation flow

    @Test
    void userCanCreateReservation_andOwnerIsTakenFromJwtNotRequest() throws Exception {
        String adminToken = login("admin", "admin123");
        String aliceToken = login("alice", "user123");
        long resourceId = createResource(adminToken, "Board Room A", "ROOM");

        String body = createReservation(aliceToken, resourceId, "250.00");
        JsonNode json = objectMapper.readTree(body);

        // The acting user is taken from the JWT, so the owner must be alice.
        assertThat(json.get("username").asText()).isEqualTo("alice");
        assertThat(json.get("status").asText()).isEqualTo("PENDING");
    }

    @Test
    void userSeesOnlyTheirOwnReservations() throws Exception {
        String adminToken = login("admin", "admin123");
        String aliceToken = login("alice", "user123");
        String bobToken = login("bob", "user123");

        ResourcePair pair = buildResourceAndReservation(adminToken, aliceToken, "150.00");
        // bob books the same resource on a different day to avoid a conflict
        long bobResourceId = createResource(adminToken, "Projector", "EQUIPMENT");
        createReservation(bobToken, bobResourceId, "300.00");

        // alice sees only her own reservation
        mockMvc.perform(get("/api/reservations").header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].username").value("alice"));

        // bob cannot see alice's reservation (404 hides existence)
        mockMvc.perform(get("/api/reservations/" + pair.reservationId())
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isNotFound());

        // admin sees all reservations
        mockMvc.perform(get("/api/reservations").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void userCanOnlyCancelOwnReservation() throws Exception {
        String adminToken = login("admin", "admin123");
        String aliceToken = login("alice", "user123");
        ResourcePair pair = buildResourceAndReservation(adminToken, aliceToken, "150.00");

        // USER cannot change the price
        mockMvc.perform(patch("/api/reservations/" + pair.reservationId())
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"price\":999}"))
                .andExpect(status().isForbidden());

        // USER cannot set a status other than CANCELLED on their own
        mockMvc.perform(patch("/api/reservations/" + pair.reservationId())
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CONFIRMED\"}"))
                .andExpect(status().isForbidden());

        // USER cannot touch another user's reservation
        String bobToken = login("bob", "user123");
        mockMvc.perform(patch("/api/reservations/" + pair.reservationId())
                        .header("Authorization", "Bearer " + bobToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void cancelOwnReservation_returns200AndUpdatesStatus() throws Exception {
        String adminToken = login("admin", "admin123");
        String aliceToken = login("alice", "user123");
        ResourcePair pair = buildResourceAndReservation(adminToken, aliceToken, "150.00");

        mockMvc.perform(patch("/api/reservations/" + pair.reservationId())
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void adminCanUpdateReservationToAnyStatus() throws Exception {
        String adminToken = login("admin", "admin123");
        String aliceToken = login("alice", "user123");
        ResourcePair pair = buildResourceAndReservation(adminToken, aliceToken, "150.00");

        mockMvc.perform(patch("/api/reservations/" + pair.reservationId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CONFIRMED\",\"price\":175.5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.price").value(175.5));
    }

    // ------------------------------------------- Filtering / pagination / sorting

    @Test
    void filterReservationsByStatusMinPriceAndMaxPrice_andPaginate() throws Exception {
        String adminToken = login("admin", "admin123");
        String aliceToken = login("alice", "user123");
        long resourceId = createResource(adminToken, "Board Room A", "ROOM");

        // 3 reservations for alice with distinct prices/status and non-overlapping windows
        createReservationAt(aliceToken, resourceId, "100.00", "PENDING", 1);
        String confirmed = createReservationAt(aliceToken, resourceId, "200.00", "CONFIRMED", 2);
        String cancelled = createReservationAt(aliceToken, resourceId, "500.00", "CANCELLED", 3);
        long confirmedId = objectMapper.readTree(confirmed).get("id").asLong();
        long cancelledId = objectMapper.readTree(cancelled).get("id").asLong();

        // filter by status = CONFIRMED
        mockMvc.perform(get("/api/reservations")
                        .param("status", "CONFIRMED")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(confirmedId));

        // filter by minPrice >= 150 -> confirmed(200) + cancelled(500)
        mockMvc.perform(get("/api/reservations")
                        .param("minPrice", "150")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));

        // filter by status = PENDING and maxPrice <= 200 -> pending(100)
        mockMvc.perform(get("/api/reservations")
                        .param("maxPrice", "200")
                        .param("status", "PENDING")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        // pagination page size 1 -> 3 pages
        mockMvc.perform(get("/api/reservations")
                        .param("page", "0").param("size", "1")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.numberOfElements").value(1));

        // sorting by price ascending -> cheapest first (100)
        MvcResult sorted = mockMvc.perform(get("/api/reservations")
                        .param("sort", "price,asc")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode sortedJson = objectMapper.readTree(sorted.getResponse().getContentAsString());
        assertThat(sortedJson.get("content").get(0).get("price").decimalValue())
                .isEqualByComparingTo(new BigDecimal("100.00"));

        // invalid status -> 400
        mockMvc.perform(get("/api/reservations")
                        .param("status", "BOGUS")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isBadRequest());

        // invalid sort field -> 400
        mockMvc.perform(get("/api/reservations")
                        .param("sort", "nope,asc")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isBadRequest());

        assertThat(cancelledId).isGreaterThan(0);
    }

    @Test
    void cannotDoubleBookResource() throws Exception {
        String adminToken = login("admin", "admin123");
        String aliceToken = login("alice", "user123");
        String bobToken = login("bob", "user123");
        long resourceId = createResource(adminToken, "Board Room A", "ROOM");

        createReservation(aliceToken, resourceId, "100.00");
        // bob tries to book the same resource in an overlapping window -> 409
        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + bobToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reservationBody(resourceId, "100.00", "PENDING")))
                .andExpect(status().isConflict());
    }

    // ------------------------------------------------------------------ helpers

    private String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    private long createResource(String token, String name, String type) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/resources")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"type\":\"" + type + "\",\"available\":true}"))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private String createReservation(String token, long resourceId, String price) throws Exception {
        return createReservationAt(token, resourceId, price, "PENDING", 1);
    }

    private String createReservationWithStatus(String token, long resourceId, String price, String status)
            throws Exception {
        return createReservationAt(token, resourceId, price, status, 1);
    }

    private String createReservationAt(String token, long resourceId, String price, String status, int dayOffset)
            throws Exception {
        MvcResult result = mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reservationBody(resourceId, price, status, dayOffset)))
                .andExpect(status().isCreated())
                .andReturn();
        return result.getResponse().getContentAsString();
    }

    private String reservationBody(long resourceId, String price, String status) {
        return reservationBody(resourceId, price, status, 1);
    }

    private String reservationBody(long resourceId, String price, String status, int dayOffset) {
        LocalDateTime start = LocalDateTime.now().plusDays(dayOffset).withHour(9).withMinute(0);
        LocalDateTime end = start.plusHours(2);
        return "{"
                + "\"resourceId\":" + resourceId + ","
                + "\"startTime\":\"" + start.format(ISO) + "\","
                + "\"endTime\":\"" + end.format(ISO) + "\","
                + "\"price\":" + price + ","
                + "\"status\":\"" + status + "\""
                + "}";
    }

    /** Creates a resource and one reservation for that resource; returns both ids. */
    private ResourcePair buildResourceAndReservation(String adminToken, String userToken, String price)
            throws Exception {
        long resourceId = createResource(adminToken, "Board Room A", "ROOM");
        String body = createReservation(userToken, resourceId, price);
        long reservationId = objectMapper.readTree(body).get("id").asLong();
        return new ResourcePair(resourceId, reservationId);
    }

    private void seedUser(String username, String email, String rawPassword, String fullName, Role role) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setFullName(fullName);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        userRepository.save(user);
    }

    private record ResourcePair(long resourceId, long reservationId) {
    }
}
