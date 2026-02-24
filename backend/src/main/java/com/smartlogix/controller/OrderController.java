package com.smartlogix.controller;

import com.smartlogix.domain.enums.OrderStatus;
import com.smartlogix.dto.OrderRequestDTO;
import com.smartlogix.dto.OrderResponseDTO;
import com.smartlogix.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "Orders", description = "Order management endpoints")
public class OrderController {

    private final OrderService orderService;
    private final JobLauncher jobLauncher;
    private final Job orderImportJob;

    /**
     * Returns a paginated list of orders for the currently authenticated tenant.
     *
     * @param pageable Spring Data {@link Pageable} containing page number, size, and sort
     * @return {@link ResponseEntity} wrapping a {@link Page} of {@link OrderResponseDTO} objects
     */
    @GetMapping
    @Operation(summary = "List orders for current tenant")
    public ResponseEntity<Page<OrderResponseDTO>> getOrders(Pageable pageable) {
        return ResponseEntity.ok(orderService.getOrders(pageable));
    }

    /**
     * Creates a new order for the currently authenticated tenant.
     * <p>
     * The request body is validated via Jakarta Bean Validation before being forwarded to the
     * service layer. The order is automatically linked to the tenant extracted from the JWT.
     * </p>
     *
     * @param requestDTO the validated order creation payload
     * @return {@link ResponseEntity} containing the persisted {@link OrderResponseDTO}
     */
    @PostMapping
    @Operation(summary = "Create a new order")
    public ResponseEntity<OrderResponseDTO> createOrder(@Valid @RequestBody OrderRequestDTO requestDTO) {
        return ResponseEntity.ok(orderService.createOrder(requestDTO));
    }

    /**
     * Retrieves a single order by its UUID, scoped to the current tenant.
     *
     * @param id the UUID of the order to fetch
     * @return {@link ResponseEntity} containing the matching {@link OrderResponseDTO}
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get order by ID")
    public ResponseEntity<OrderResponseDTO> getOrderById(@PathVariable UUID id) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    /**
     * Transitions the status of an order following the enforced state-machine rules.
     * <p>
     * Expects a JSON body of the form {@code {"newStatus": "APPROVED"}}. The service layer
     * validates that the transition is legal; if not, a {@code 400 Bad Request} is returned.
     * On success, a Kafka event is published and connected WebSocket clients are notified.
     * </p>
     *
     * @param id   the UUID of the order to update
     * @param body request body map containing the {@code newStatus} key
     * @return {@link ResponseEntity} containing the updated {@link OrderResponseDTO}
     */
    @PatchMapping("/{id}/status")
    @Operation(summary = "Transition order status")
    public ResponseEntity<OrderResponseDTO> transitionStatus(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        OrderStatus newStatus = OrderStatus.valueOf(body.get("newStatus"));
        return ResponseEntity.ok(orderService.transitionStatus(id, newStatus));
    }

    /**
     * Accepts a CSV file upload and triggers a Spring Batch job to import orders in bulk.
     * <p>
     * The uploaded file is written to a temporary location on disk and the path is passed to
     * the {@code orderImportJob} as a job parameter. The job reads records in chunks of 10,
     * validates each row via {@link com.smartlogix.batch.OrderItemProcessor}, and persists
     * valid orders to the database. The endpoint returns immediately — import processing
     * continues asynchronously in the batch job thread.
     * </p>
     *
     * @param file the multipart CSV file containing order records
     * @return {@link ResponseEntity} with a confirmation message if the job was launched,
     *         or a {@code 500} response if launching failed
     */
    @PostMapping("/import")
    @Operation(summary = "Import orders from CSV file")
    public ResponseEntity<Map<String, String>> importOrders(@RequestParam("file") MultipartFile file) {
        try {
            File tempFile = Files.createTempFile("order-import-", ".csv").toFile();
            file.transferTo(tempFile);
            tempFile.deleteOnExit();

            jobLauncher.run(orderImportJob, new JobParametersBuilder()
                    .addString("filePath", tempFile.getAbsolutePath())
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters());
            return ResponseEntity.ok(Map.of("message", "Import job started successfully"));
        } catch (Exception e) {
            log.error("Failed to start import job", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "Failed to start import: " + e.getMessage()));
        }
    }
}
