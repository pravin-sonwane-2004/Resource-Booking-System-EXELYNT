# Resource Booking API

A secure, RESTful **Resource Booking System** built with **Spring Boot 4**, **Java 21+**, **Spring Security (JWT)**, **Spring Data JPA/Hibernate**, and **MySQL**.

It lets regular users view available resources and create/manage their own reservations, while administrators get full CRUD access to manage resources and all reservations.

---

## Features

- **JWT authentication** via `POST /auth/login` (stateless, BCrypt-hashed passwords).
- **RBAC** with `ADMIN` and `USER` roles through Spring Security URL authorization.
  - `ADMIN` — full CRUD on resources and reservations.
  - `USER` — read-only access to resources; can create reservations and **view/update only their own**.
- **Identity from the JWT** — the reservation owner is always derived from the authenticated token, never from the request body.
- Reservation **statuses**: `PENDING`, `CONFIRMED`, `CANCELLED`.
- Reservation **price** stored as a `DECIMAL(10,2)`.
- **Filtering** by `status`, `minPrice`, `maxPrice`.
- **Pagination** (`page`, `size`) and **sorting** (`sort=field,dir`).
- **ADMIN views all** reservations; **USER views only their own** (ownership enforced in the service layer).
- Input **validation** (Jakarta Bean Validation) with consistent, meaningful JSON error responses.
- **Swagger/OpenAPI UI** for interactive API documentation and testing.
- **Seed users** and sample data created automatically on startup.
- Overlap detection prevents double-booking a resource.

---

## Tech Stack

| Layer      | Technology                                        |
|------------|---------------------------------------------------|
| Language   | Java 21+                                          |
| Framework  | Spring Boot 4.1.1 (MVC, Security, Data JPA)       |
| Security   | Spring Security, JJWT 0.12, BCrypt                |
| Database   | MySQL (H2 in-memory for tests)                    |
| ORM        | Hibernate / Spring Data JPA                       |
| Boilerplate| Project Lombok (getters/setters/constructors)     |
| Validation | Jakarta Bean Validation                           |
| Docs       | springdoc-openapi (Swagger UI)                    |
| Build      | Maven                                            |

---

## Prerequisites

- **JDK 21+** (developed and tested on JDK 26)
- **Maven 3.9+**
- **MySQL 8+** (or any MySQL-compatible server)

---

## Quick Start

### 1. Create the database

```sql
CREATE DATABASE IF NOT EXISTS exelynt;
```

### 2. Configure via environment variables (optional)

All configuration has sensible defaults, but can be overridden with environment
variables:

| Environment variable             | Default     | Description                                   |
|----------------------------------|-------------|-----------------------------------------------|
| `SERVER_PORT`                    | `8080`      | HTTP port                                     |
| `SPRING_DATASOURCE_URL`          | `jdbc:mysql://localhost:3306/exelynt?...` | JDBC URL |
| `SPRING_DATASOURCE_USERNAME`     | `root`      | DB username                                   |
| `SPRING_DATASOURCE_PASSWORD`     | `0000`      | DB password                                   |
| `SPRING_JPA_HIBERNATE_DDL_AUTO`  | `update`    | `update`, `create`, `validate`, `none`        |
| `SPRING_JPA_SHOW_SQL`            | `false`     | Log generated SQL                             |
| `JWT_SECRET`                     | *see below* | Base64 secret ≥ 256 bits (HS256)              |
| `JWT_EXPIRATION`                 | `86400000`  | Token validity in ms (24h)                    |

> **Important:** change `JWT_SECRET` to your own value in production. It must be a
> Base64-encoded string representing at least 256 bits. Generate one, e.g.:
> `openssl rand -base64 48`

### 3. Run

```bash
# from the project root
mvn spring-boot:run
```

Or build and run the packaged jar:

```bash
mvn clean package
java -jar target/Resource-Booking-0.0.1-SNAPSHOT.jar
```

### 4. Open Swagger UI

- Swagger UI: <http://localhost:8080/swagger-ui.html>
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>

Use the **Authorize** button and paste `Bearer <token>` from login to call the
secured endpoints.

---

## Seed Users

The application seeds the following accounts on startup (passwords are BCrypt-hashed):

| Role    | Username    | Email                  | Password     | Permissions                                          |
|---------|-------------|------------------------|--------------|------------------------------------------------------|
| ADMIN   | `admin`     | admin@booking.com      | `admin123`   | Full CRUD on resources & all reservations            |
| USER    | `user`      | user@booking.com       | `user123`    | Read resources; manage own reservations              |
| USER    | `jane`      | jane@booking.com       | `user123`    | Read resources; manage own reservations              |

---

## API Endpoints

### Authentication

| Method | Path              | Description                                  | Access        |
|--------|-------------------|----------------------------------------------|---------------|
| POST   | `/auth/login`     | Log in, returns a JWT                        | Public        |
| POST   | `/auth/register`  | Register a new **USER** account              | Public        |

`POST /auth/login` request body:

```json
{ "username": "admin", "password": "admin123" }
```

Response:

```json
{
  "token": "<jwt>",
  "tokenType": "Bearer",
  "expiresIn": 86400000,
  "id": 1,
  "username": "admin",
  "fullName": "System Administrator",
  "email": "admin@booking.com",
  "role": "ADMIN"
}
```

### Resources

| Method | Path                | Description          | Access |
|--------|---------------------|----------------------|--------|
| GET    | `/api/resources`    | List all resources   | ADMIN, USER |
| GET    | `/api/resources/{id}` | Get one resource   | ADMIN, USER |
| POST   | `/api/resources`    | Create a resource    | ADMIN  |
| PUT    | `/api/resources/{id}` | Update a resource  | ADMIN  |
| DELETE | `/api/resources/{id}` | Delete a resource | ADMIN  |

Resource payload:

```json
{
  "name": "Board Room A",
  "description": "Large meeting room with projector",
  "type": "ROOM",
  "available": true
}
```

`type` is one of: `ROOM`, `VEHICLE`, `EQUIPMENT`, `OTHER`.

### Reservations

| Method | Path                | Description          | Access |
|--------|---------------------|----------------------|--------|
| GET    | `/api/reservations` | List (filter/paginate/sort) | ADMIN: all; USER: own |
| GET    | `/api/reservations/{id}` | Get one           | ADMIN: all; USER: own |
| POST   | `/api/reservations` | Create a reservation | ADMIN, USER |
| PATCH  | `/api/reservations/{id}` | Update/cancel   | ADMIN: full; USER: cancel own only |
| DELETE | `/api/reservations/{id}` | Delete           | ADMIN: all; USER: own |

Create/update payload:

```json
{
  "resourceId": 1,
  "startTime": "2026-09-01T09:00:00",
  "endTime": "2026-09-01T11:00:00",
  "price": 150.00,
  "status": "PENDING"
}
```

> `status` is optional on create (defaults to `PENDING`). A regular `USER` may only
> set status to `CANCELLED` when updating their own reservation; `ADMIN` can set any
> status, and change price and time window.

#### Filtering, pagination & sorting (`GET /api/reservations`)

| Query param | Example            | Description                         |
|-------------|--------------------|-------------------------------------|
| `status`    | `CONFIRMED`        | Filter by reservation status        |
| `minPrice`  | `150`              | Minimum price (inclusive)           |
---

## Error Handling

All errors are returned as a consistent JSON body:

```json
{
  "timestamp": "2026-08-29T10:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/reservations",
  "fieldErrors": {
    "price": "Price must be zero or greater"
  }
}
```

| HTTP Status | Meaning                                                      |
|-------------|--------------------------------------------------------------|
| `400`       | Validation failed / malformed request / invalid filter       |
| `401`       | Missing/invalid credentials or token                         |
| `403`       | Authenticated but not permitted (e.g. USER writing resources)|
| `404`       | Resource/reservation not found (hides others' reservations)  |
| `409`       | Conflict — resource already booked in that window            |
| `500`       | Unexpected server error                                      |

---

## Project Structure

```
src/main/java/com/pravin/Resource_Booking/
├── config/            OpenAPI config + seed data initializer
├── controller/        Auth, Resource, Reservation REST controllers
├── dto/               Request/response records (auth, resource, reservation, common, user)
├── entity/            User, Resource, Reservation + enums (Role, ResourceType, ReservationStatus)
├── exception/         Custom exceptions + GlobalExceptionHandler
├── repository/        Spring Data JPA repositories
├── security/          JwtService, JWT filter, UserDetails, SecurityConfig
└── service/           AuthService, ResourceService, ReservationService (business + ownership rules)
```

Layers are cleanly separated: **Controller → Service → Repository**, with **DTOs**
exposed to the API and JPA **entities** kept internal.

---

## Security Notes

- **Stateless** JWT auth — no server-side session.
- Passwords stored with **BCrypt**.
- Ownership is enforced in `ReservationService` using `SecurityContextHolder`
  (the authenticated `CustomUserDetails`), so a `USER` can never access another
  user's reservation even if they guess the id (returns 404).
- Cross-user data leakage is further prevented by scoping the list query to the
  user's id for `USER` role.
- Self-registration always creates a `USER`; `ADMIN` accounts are provisioned via
  the seed data.

---

## Testing

The suite runs on an in-memory **H2** database (`test` profile) with a real security
filter chain and covers authentication, RBAC, ownership, filtering, pagination,
sorting, and double-booking prevention:

```bash
mvn test
```

Included tests:

- Login success / invalid credentials / missing fields
- Unauthorized access without token (401)
- USER forbidden from creating/modifying resources (403)
- ADMIN full resource CRUD
- Reservation creation owner derived from JWT
- USER sees only own reservations; ADMIN sees all
- USER can only cancel own reservation (403 on price/status changes, 404 on others')
- ADMIN can update any reservation
- Filtering by status/minPrice/maxPrice, pagination, sorting
- Invalid status / sort field → 400
- Double-booking a resource → 409

---

## Environment Variables (`.env.example`)

```bash
# Server
SERVER_PORT=8080

# Database (MySQL)
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/exelynt?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
SPRING_DATASOURCE_USERNAME=root
SPRING_DATASOURCE_PASSWORD=0000
SPRING_JPA_HIBERNATE_DDL_AUTO=update

# JWT (Base64 secret >= 256 bits)
JWT_SECRET=<base64-secret>
JWT_EXPIRATION=86400000
```

> A default Base64 secret is provided for local development. **Always override it in
> production.**

| `maxPrice`  | `500`              | Maximum price (inclusive)           |
| `page`      | `0`                | Zero-based page index               |
| `size`      | `20`               | Page size                           |
| `sort`      | `createdAt,desc`   | Sort field(s) and direction         |

Examples:

```bash
# All reservations, page 0, 10 per page, newest first
GET /api/reservations?page=0&size=10&sort=createdAt,desc

# Confirmed reservations under $200
GET /api/reservations?status=CONFIRMED&maxPrice=200

# Reservations priced $100 - $500
GET /api/reservations?minPrice=100&maxPrice=500
```

