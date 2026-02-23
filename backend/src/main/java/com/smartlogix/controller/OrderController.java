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

    @GetMapping
    @Operation(summary = "List orders for current tenant")
    public ResponseEntity<Page<OrderResponseDTO>> getOrders(Pageable pageable) {
        return ResponseEntity.ok(orderService.getOrders(pageable));
    }

    @PostMapping
    @Operation(summary = "Create a new order")
    public ResponseEntity<OrderResponseDTO> createOrder(@Valid @RequestBody OrderRequestDTO requestDTO) {
        return ResponseEntity.ok(orderService.createOrder(requestDTO));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order by ID")
    public ResponseEntity<OrderResponseDTO> getOrderById(@PathVariable UUID id) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Transition order status")
    public ResponseEntity<OrderResponseDTO> transitionStatus(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        OrderStatus newStatus = OrderStatus.valueOf(body.get("newStatus"));
        return ResponseEntity.ok(orderService.transitionStatus(id, newStatus));
    }

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
