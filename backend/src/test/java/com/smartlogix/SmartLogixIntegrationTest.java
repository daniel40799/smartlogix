package com.smartlogix;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartlogix.dto.AuthRequest;
import com.smartlogix.dto.AuthResponse;
import com.smartlogix.dto.OrderRequestDTO;
import com.smartlogix.dto.OrderResponseDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
class SmartLogixIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:15-alpine"))
            .withDatabaseName("smartlogix_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.cloud.stream.kafka.binder.brokers", kafka::getBootstrapServers);
    }

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    ObjectMapper objectMapper;

    private String baseUrl() {
        return "http://localhost:" + port;
    }

    @Test
    void testRegisterAndLogin() {
        // Register a new user
        Map<String, String> registerBody = Map.of(
                "email", "test@example.com",
                "password", "password123",
                "tenantSlug", "test-tenant"
        );
        ResponseEntity<AuthResponse> registerResponse = restTemplate.postForEntity(
                baseUrl() + "/api/auth/register",
                registerBody,
                AuthResponse.class
        );
        assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(registerResponse.getBody()).isNotNull();
        assertThat(registerResponse.getBody().getToken()).isNotBlank();
        assertThat(registerResponse.getBody().getEmail()).isEqualTo("test@example.com");

        // Login with same credentials
        AuthRequest loginRequest = new AuthRequest("test@example.com", "password123");
        ResponseEntity<AuthResponse> loginResponse = restTemplate.postForEntity(
                baseUrl() + "/api/auth/login",
                loginRequest,
                AuthResponse.class
        );
        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(loginResponse.getBody()).isNotNull();
        assertThat(loginResponse.getBody().getToken()).isNotBlank();
    }

    @Test
    void testCreateOrderAndTransitionStatus() {
        // Register user
        Map<String, String> registerBody = Map.of(
                "email", "order-test@example.com",
                "password", "password123",
                "tenantSlug", "order-tenant"
        );
        ResponseEntity<AuthResponse> registerResponse = restTemplate.postForEntity(
                baseUrl() + "/api/auth/register",
                registerBody,
                AuthResponse.class
        );
        assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        String token = registerResponse.getBody().getToken();

        // Create order
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        OrderRequestDTO orderRequest = OrderRequestDTO.builder()
                .orderNumber("ORD-001")
                .description("Test order")
                .destinationAddress("123 Main St")
                .weight(new BigDecimal("5.50"))
                .build();

        ResponseEntity<OrderResponseDTO> createResponse = restTemplate.exchange(
                baseUrl() + "/api/orders",
                HttpMethod.POST,
                new HttpEntity<>(orderRequest, headers),
                OrderResponseDTO.class
        );
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(createResponse.getBody()).isNotNull();
        assertThat(createResponse.getBody().getOrderNumber()).isEqualTo("ORD-001");
        assertThat(createResponse.getBody().getStatus().name()).isEqualTo("PENDING");

        // Transition status to APPROVED
        Map<String, String> statusBody = Map.of("newStatus", "APPROVED");
        ResponseEntity<OrderResponseDTO> approveResponse = restTemplate.exchange(
                baseUrl() + "/api/orders/" + createResponse.getBody().getId() + "/status",
                HttpMethod.PATCH,
                new HttpEntity<>(statusBody, headers),
                OrderResponseDTO.class
        );
        assertThat(approveResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(approveResponse.getBody().getStatus().name()).isEqualTo("APPROVED");
    }

    @Test
    void testGetOrdersRequiresAuth() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                baseUrl() + "/api/orders",
                String.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
