package com.smartlogix.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for the {@code POST /api/auth/login} endpoint.
 * <p>
 * Both fields are required and validated via Jakarta Bean Validation before the request
 * reaches the service layer.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthRequest {

    /** The user's email address used as the login identifier. Must be a valid email format. */
    @Email(message = "Valid email is required")
    @NotBlank(message = "Email is required")
    private String email;

    /** The user's plain-text password. Matched against the stored BCrypt hash at login time. */
    @NotBlank(message = "Password is required")
    private String password;
}
