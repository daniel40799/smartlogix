# 🚚 SmartLogix — Multi-Tenant Order Management Platform

A production-grade SaaS logistics platform built with Spring Boot 3 and React + Redux. SmartLogix provides a unified "single pane of glass" for logistics companies to manage their entire shipment lifecycle with real-time visibility.

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                    React + Redux Frontend                    │
│   Dashboard │ Orders │ Map View │ Live WebSocket Alerts     │
└─────────────────────────┬───────────────────────────────────┘
                          │ REST + WebSocket (STOMP)
┌─────────────────────────▼───────────────────────────────────┐
│               Spring Boot 3.3 Backend (Java 17)             │
│                                                              │
│  JWT Auth ──► TenantContext ──► Multi-Tenant Data Isolation  │
│                                                              │
│  OrderService (State Machine)                                │
│  PENDING → APPROVED → IN_TRANSIT → SHIPPED → DELIVERED      │
│                    └──────────────────► CANCELLED            │
│                                                              │
│  Spring Batch (CSV bulk import)                              │
│  Spring Integration (FTP polling)                            │
│  Spring Data Envers (audit trail)                            │
│  Spring Cloud Stream → Kafka (event-driven)                  │
└──────┬───────────────────────────┬───────────────────────────┘
       │                           │
┌──────▼───────┐        ┌─────────▼──────────┐
│  PostgreSQL  │        │  Apache Kafka        │
│  (B-Tree idx │        │  (OrderCreated,      │
│  tenant_id,  │        │  StatusChanged       │
│  order_status│        │  events)             │
└──────────────┘        └────────────────────┘
```

---

## Tech Stack

| Category | Technology | Purpose |
|---|---|---|
| **Language** | Java 17 | Backend runtime |
| **Framework** | Spring Boot 3.3.4 | Application foundation |
| **Security** | Spring Security + JWT (jjwt) | Multi-tenant auth & isolation |
| **Persistence** | Spring Data JPA + Hibernate | ORM with B-Tree indexes |
| **Audit** | Spring Data Envers | Full change history / compliance |
| **Messaging** | Spring Cloud Stream + Kafka | Event-driven architecture |
| **WebSocket** | Spring WebSocket (STOMP) | Live order status updates |
| **Batch** | Spring Batch | Bulk CSV/order imports |
| **Integration** | Spring Integration (FTP) | Legacy system ingestion |
| **Mapping** | MapStruct | Type-safe entity↔DTO mapping |
| **Utilities** | Lombok, Jackson | Code generation, JSON |
| **Validation** | Jakarta Bean Validation | Input validation |
| **API Docs** | SpringDoc OpenAPI 3 | Swagger UI |
| **Observability** | Actuator + Micrometer + Prometheus | Metrics & health |
| **Logging** | Logback + logstash-logback-encoder | Structured JSON logs |
| **Testing** | JUnit 5 + Mockito + TestContainers | Unit + Integration tests |
| **Frontend** | React 18 + Redux Toolkit + TypeScript | SPA dashboard |
| **State Mgmt** | @reduxjs/toolkit | Predictable state |
| **Maps** | React Leaflet + OpenStreetMap | Shipment location view |
| **Database** | PostgreSQL 15 | Primary data store |
| **Broker** | Apache Kafka (Confluent) | Event streaming |
| **Container** | Docker + Docker Compose | Local dev environment |
| **CI/CD** | GitHub Actions | Build, test, deploy pipeline |

---

## Key Features

### 🏢 Multi-Tenancy
- **Shared Database, Separate Data** — every entity is scoped by `tenant_id`
- JWT tokens carry `tenant_id` claims, extracted by `JwtAuthFilter` into `TenantContext` (ThreadLocal)
- Company A can never see Company B's data

### 📦 Order State Machine
Orders follow a strict lifecycle enforced by `OrderService`:
```
PENDING → APPROVED → IN_TRANSIT → SHIPPED → DELIVERED
   └──────────┴───────────┴──────────┘
                    CANCELLED (terminal)
```
Invalid transitions throw `IllegalStateException` (400 Bad Request).

### ⚡ Event-Driven Architecture
When an order changes status, `OrderEventProducer` publishes to Kafka. The `OrderEventConsumerConfig` bean consumes events and broadcasts via WebSocket to `/topic/orders/{tenantId}`.

### 📡 Real-Time WebSocket Updates
The React frontend uses `@stomp/stompjs` + SockJS to subscribe to `/topic/orders/{tenantId}`. Status changes appear instantly without page refresh.

### 📊 Spring Batch — Bulk CSV Import
`POST /api/orders/import` accepts a CSV file and processes it in chunks via a Spring Batch job, handling transactional restartability.

### 🔌 Spring Integration — FTP Ingestion
`FtpIntegrationConfig` (enabled via `smartlogix.integration.ftp.enabled=true`) polls an FTP directory for shipment manifests and feeds them into the event pipeline.

### 📜 Audit Trail (Envers)
Every `Order` change is snapshotted in `orders_aud` tables. Full revision history is available via `RevisionRepository`.

---

## Getting Started

### Prerequisites
- Docker & Docker Compose
- Java 17+ (for running tests locally)
- Node.js 20+ (for frontend development)

### Quick Start with Docker Compose
```bash
# Clone the repository
git clone https://github.com/daniel40799/smartlogix
cd smartlogix

# Start all services (Postgres, Kafka, Backend, Frontend)
docker compose up -d

# The API is available at: http://localhost:8080
# The frontend is available at: http://localhost:3000
# Swagger UI: http://localhost:8080/swagger-ui.html
# Actuator health: http://localhost:8080/actuator/health
# Prometheus metrics: http://localhost:8080/actuator/prometheus
```

### Local Development

**Backend:**
```bash
# Start infrastructure only
docker compose up -d postgres zookeeper kafka

# Run the backend
cd backend
mvn spring-boot:run
```

**Frontend:**
```bash
cd frontend
npm install
npm run dev
# Frontend at http://localhost:3000 (proxied to backend at :8080)
```

**Run Tests:**
```bash
cd backend
# Unit tests (fast, no Docker needed)
mvn test -Dtest=OrderServiceTest

# Integration tests (requires Docker for TestContainers)
mvn test -Dtest=SmartLogixIntegrationTest

# All tests
mvn verify
```

---

## API Reference

### Authentication
| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/auth/register` | Register company + admin user |
| `POST` | `/api/auth/login` | Get JWT token |

### Orders
| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/orders` | List orders (paginated, tenant-scoped) |
| `POST` | `/api/orders` | Create order |
| `GET` | `/api/orders/{id}` | Get order by ID |
| `PATCH` | `/api/orders/{id}/status` | Transition order status |
| `POST` | `/api/orders/import` | Bulk CSV import (Spring Batch) |

### Observability
| Endpoint | Description |
|---|---|
| `/actuator/health` | Application health |
| `/actuator/metrics` | All metrics |
| `/actuator/prometheus` | Prometheus scrape endpoint |
| `/swagger-ui.html` | API documentation |

---

## Project Structure

```
smartlogix/
├── backend/                          # Spring Boot application
│   ├── src/main/java/com/smartlogix/
│   │   ├── SmartLogixApplication.java
│   │   ├── batch/                    # Spring Batch CSV processing
│   │   ├── config/                   # Security, WebSocket, Batch, OpenAPI configs
│   │   ├── controller/               # REST controllers
│   │   ├── domain/
│   │   │   ├── entity/               # JPA entities (Order, Tenant, User)
│   │   │   ├── enums/                # OrderStatus, UserRole
│   │   │   └── repository/           # Spring Data repositories
│   │   ├── dto/                      # Request/Response DTOs
│   │   ├── exception/                # Global exception handling
│   │   ├── integration/              # Spring Integration FTP config
│   │   ├── mapper/                   # MapStruct mappers
│   │   ├── messaging/                # Kafka event producer/consumer
│   │   ├── security/                 # JWT, TenantContext, UserDetailsService
│   │   └── service/                  # Business logic (OrderService, AuthService)
│   ├── src/test/                     # Unit + Integration tests (TestContainers)
│   ├── Dockerfile
│   └── pom.xml
│
├── frontend/                         # React + Redux TypeScript SPA
│   ├── src/
│   │   ├── api/                      # Axios API clients
│   │   ├── components/               # Reusable components (Navbar)
│   │   ├── hooks/                    # useWebSocket hook
│   │   ├── pages/                    # Dashboard, Orders, Map, Auth pages
│   │   ├── store/                    # Redux store + slices
│   │   └── types/                    # TypeScript types
│   ├── Dockerfile
│   ├── nginx.conf
│   └── package.json
│
├── .github/workflows/
│   └── ci.yml                        # GitHub Actions CI/CD
├── docker-compose.yml                # Full local stack
└── README.md
```

---

## Security Notes

- JWT tokens are signed with a configurable secret (`SMARTLOGIX_JWT_SECRET` env var)
- All endpoints except `/api/auth/**`, `/swagger-ui/**`, `/v3/api-docs/**`, `/actuator/**`, `/ws/**` require authentication
- CSRF protection is intentionally disabled — the API uses stateless JWT Bearer tokens (not cookies), making CSRF inapplicable
- Passwords are stored as BCrypt hashes
- Multi-tenancy is enforced at the service layer via `TenantContext`

---

## What Was Built — Engineering Highlights

This section maps the implemented features to the engineering practices described in the role requirements for **Senior Full Stack Engineer** (8×8) and **Senior Backend Engineer** (Kuehne+Nagel).

### Full-Stack Engineering (React + Spring Boot)

| Requirement | Implementation |
|---|---|
| Modern, responsive React UI | React 18 + Redux Toolkit SPA with TypeScript, covering Dashboard, Orders, Map, and Auth pages |
| Redux state management | `@reduxjs/toolkit` slices manage authentication state and order data across the app |
| Live updates without page refresh | `useWebSocket` hook uses `@stomp/stompjs` + SockJS to subscribe to `/topic/orders/{tenantId}`; status changes appear instantly |
| RESTful JSON API | Spring Boot 3 controllers expose clean REST endpoints with proper HTTP semantics (GET / POST / PATCH) |
| Input validation | Jakarta Bean Validation (`@NotBlank`, `@NotNull`, etc.) on DTOs; `GlobalExceptionHandler` returns field-level errors |
| OpenAPI documentation | SpringDoc OpenAPI 3 generates Swagger UI at `/swagger-ui.html`; every endpoint is annotated with `@Operation` and `@Tag` |

### Security & JWT / OAuth Alignment

| Requirement | Implementation |
|---|---|
| JWT authentication | `JwtUtil` generates HMAC-SHA256 signed tokens containing `email`, `tenantId`, and `role` claims |
| Stateless auth filter | `JwtAuthFilter` (extends `OncePerRequestFilter`) extracts and validates the `Bearer` token on every request |
| Multi-tenant isolation | `TenantContext` (ThreadLocal) carries the tenant UUID from the filter to the service/repository layer — Company A can never see Company B's data |
| Role-based access control | `@PreAuthorize("isAuthenticated()")` on controllers; roles embedded in JWT and loaded by `CustomUserDetailsService` |
| Secure password storage | BCrypt encoding via Spring Security `PasswordEncoder` |

### Java Backend Engineering (Spring Ecosystem)

| Requirement | Implementation |
|---|---|
| Spring Boot / Spring Core | Spring Boot 3.3 application with full auto-configuration, profiles, and property binding |
| Spring Data JPA | `OrderRepository`, `TenantRepository`, `UserRepository` with derived query methods and `@Query` annotations; B-Tree indexes on `tenant_id` and `order_status` |
| Spring Data Envers | `@Audited` on the `Order` entity — every change is snapshotted in `orders_aud` for a full audit trail / compliance |
| Spring Batch | `BatchConfig` defines a chunk-oriented (size 10) import job; `OrderItemProcessor` validates and maps CSV rows; transactional restartability is built in |
| Spring Integration (FTP) | `FtpIntegrationConfig` polls an FTP server every 30 seconds, downloads shipment manifest files to a local staging directory, and feeds them into the event pipeline |
| Spring Cloud Stream + Kafka | `OrderEventProducer` publishes `OrderCreated` / `OrderStatusChanged` events via `StreamBridge`; `OrderEventConsumerConfig` consumes those events and fans out via WebSocket |
| Spring WebSocket (STOMP) | `WebSocketConfig` registers the STOMP endpoint at `/ws` and the `/topic` message broker prefix |
| MapStruct | `OrderMapper` provides zero-reflection, compile-time verified entity ↔ DTO conversion |
| Lombok | `@Data`, `@Builder`, `@RequiredArgsConstructor`, `@Slf4j` used throughout to eliminate boilerplate |
| Jackson | JSON serialisation / deserialisation for all REST payloads and Kafka event messages |
| Jakarta Validation | Constraint annotations on DTOs; violations surfaced as structured `400` responses |

### Clean Code & Software Craftsmanship

| Requirement | Implementation |
|---|---|
| Clean Code principles | Single-responsibility services, thin controllers, no business logic in entities; each class has a focused purpose |
| Comprehensive Javadoc | Every public and private method across all service, controller, security, messaging, batch, and integration classes is documented with `@param`, `@return`, and `@throws` tags |
| Order state machine | `OrderService.validateTransition()` encodes all legal transitions in a `switch` expression; invalid transitions throw descriptive `IllegalStateException` messages |
| Global exception handling | `GlobalExceptionHandler` (`@RestControllerAdvice`) maps domain exceptions to appropriate HTTP status codes with structured error bodies |
| Structured logging | Logback + `logstash-logback-encoder` produce JSON log lines; every key operation is logged with contextual fields (orderId, tenantId, email) |
| Observability | Spring Actuator health/metrics + Micrometer + Prometheus scrape endpoint at `/actuator/prometheus` |

### Messaging & Data

| Requirement | Implementation |
|---|---|
| Apache Kafka | Spring Cloud Stream bindings; `order-events-out-0` output channel for domain events |
| RabbitMQ compatibility | Spring Cloud Stream abstraction means the binder can be swapped to RabbitMQ without changing application code |
| PostgreSQL | Primary data store; schema managed by Liquibase/Flyway-compatible DDL scripts; B-Tree composite indexes for fast tenant-scoped queries |

### DevOps & CI/CD

| Requirement | Implementation |
|---|---|
| Docker & Docker Compose | Multi-stage Dockerfiles for both backend and frontend; `docker-compose.yml` orchestrates PostgreSQL, Zookeeper, Kafka, backend, and frontend |
| GitHub Actions CI | `.github/workflows/ci.yml` builds, tests (TestContainers), and packages both backend and frontend on every push |
| Kubernetes manifests | `k8s/` directory contains Deployment and Service manifests for production cluster deployment |
| TestContainers | `SmartLogixIntegrationTest` spins up real PostgreSQL and Kafka containers for integration tests |
| k6 load tests | `k6/` directory contains load-test scripts for order creation and status transition endpoints |
