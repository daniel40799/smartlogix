package com.smartlogix.controller;

import com.smartlogix.dto.AuthRequest;
import com.smartlogix.dto.AuthResponse;
import com.smartlogix.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Auth endpoints")
public class AuthController {

    private final AuthService authService;

    /**
     * Authenticates a user and returns a signed JWT token.
     * <p>
     * The JWT contains {@code tenantId} and {@code role} claims that are used by downstream
     * filters and services for multi-tenant access control.
     * </p>
     *
     * @param request the login credentials ({@code email} and {@code password})
     * @return {@link ResponseEntity} containing an {@link AuthResponse} with the JWT token
     *         and user metadata
     */
    @PostMapping("/login")
    @Operation(summary = "Login and receive JWT token")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * Registers a new user, optionally creating the associated tenant if it does not exist.
     * <p>
     * Expects a JSON body with the keys {@code email}, {@code password}, and {@code tenantSlug}.
     * On success the user is immediately logged in and a JWT is returned.
     * </p>
     *
     * @param body map containing {@code email}, {@code password}, and {@code tenantSlug}
     * @return {@link ResponseEntity} containing an {@link AuthResponse} with the JWT token
     *         and newly created user metadata
     */
    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    public ResponseEntity<AuthResponse> register(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String password = body.get("password");
        String tenantSlug = body.get("tenantSlug");
        return ResponseEntity.ok(authService.register(email, password, tenantSlug));
    }
}
