package com.smartlogix.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Response payload returned by the {@code POST /api/auth/login} and
 * {@code POST /api/auth/register} endpoints.
 * <p>
 * Carries the signed JWT together with identifying metadata about the authenticated user
 * so that the frontend can initialise its auth state without making additional API calls.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    /** Signed HMAC-SHA256 JWT that must be included as a {@code Bearer} token in subsequent requests. */
    private String token;

    /** The authenticated user's email address. */
    private String email;

    /** UUID of the tenant this user belongs to, derived from the JWT {@code tenantId} claim. */
    private UUID tenantId;

    /** The user's role string (e.g. {@code ROLE_USER}), mirrored from the JWT {@code role} claim. */
    private String role;
}
