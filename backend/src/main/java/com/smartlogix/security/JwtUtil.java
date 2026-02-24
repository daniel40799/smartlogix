package com.smartlogix.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Slf4j
@Component
public class JwtUtil {

    @Value("${smartlogix.jwt.secret}")
    private String secret;

    @Value("${smartlogix.jwt.expiration-ms}")
    private long expirationMs;

    /**
     * Generates a signed JWT token for the given user.
     * <p>
     * The token is signed with HMAC-SHA256 using the configured secret key. It embeds the
     * following claims:
     * <ul>
     *   <li>{@code sub} — the user's email address (subject)</li>
     *   <li>{@code tenantId} — the UUID of the user's tenant (for multi-tenant routing)</li>
     *   <li>{@code role} — the user's role string (e.g. {@code ROLE_USER})</li>
     *   <li>{@code iat} — issued-at timestamp</li>
     *   <li>{@code exp} — expiration timestamp, offset by {@code smartlogix.jwt.expiration-ms}</li>
     * </ul>
     * </p>
     *
     * @param email    the user's email address, used as the JWT subject
     * @param tenantId the UUID of the tenant this user belongs to
     * @param role     the user's role string (e.g. {@code ROLE_ADMIN})
     * @return a compact, URL-safe JWT string
     */
    public String generateToken(String email, UUID tenantId, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("tenantId", tenantId.toString());
        claims.put("role", role);
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Extracts the email address (JWT subject claim) from the given token.
     *
     * @param token the compact JWT string
     * @return the email address stored in the {@code sub} claim
     */
    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extracts the tenant UUID from the {@code tenantId} claim of the given token.
     *
     * @param token the compact JWT string
     * @return the {@link UUID} identifying the tenant this token belongs to
     */
    public UUID extractTenantId(String token) {
        String tenantIdStr = extractClaim(token, claims -> claims.get("tenantId", String.class));
        return UUID.fromString(tenantIdStr);
    }

    /**
     * Extracts the role string from the {@code role} claim of the given token.
     *
     * @param token the compact JWT string
     * @return the role value (e.g. {@code ROLE_USER} or {@code ROLE_ADMIN})
     */
    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    /**
     * Validates a JWT token by checking that the subject matches the given {@link UserDetails}
     * and that the token has not expired.
     *
     * @param token       the compact JWT string to validate
     * @param userDetails the loaded user details whose username should match the token subject
     * @return {@code true} if the token is valid and not expired; {@code false} otherwise
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            final String email = extractEmail(token);
            return email.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (JwtException e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Returns {@code true} if the token's expiration date is in the past.
     *
     * @param token the compact JWT string
     * @return {@code true} if expired, {@code false} if still valid
     */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Extracts the expiration {@link Date} from the token's {@code exp} claim.
     *
     * @param token the compact JWT string
     * @return the {@link Date} at which the token expires
     */
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Generic helper that parses the token and applies a {@link java.util.function.Function}
     * to the resulting {@link Claims} object to extract a specific value.
     *
     * @param <T>            the type of the claim value to extract
     * @param token          the compact JWT string
     * @param claimsResolver a function that retrieves the desired value from {@link Claims}
     * @return the claim value returned by {@code claimsResolver}
     */
    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Parses the JWT string and returns all its claims after signature verification.
     *
     * @param token the compact JWT string
     * @return the {@link Claims} payload of the verified token
     * @throws io.jsonwebtoken.JwtException if the token is malformed or the signature is invalid
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Builds the HMAC-SHA256 {@link Key} used for signing and verifying JWT tokens.
     * <p>
     * The key is derived from the {@code smartlogix.jwt.secret} configuration property.
     * The property value must be at least 32 characters long to satisfy the HS256 algorithm's
     * minimum key-length requirement.
     * </p>
     *
     * @return a {@link Key} suitable for use with the HMAC-SHA256 algorithm
     */
    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
