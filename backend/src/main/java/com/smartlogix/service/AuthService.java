package com.smartlogix.service;

import com.smartlogix.domain.entity.Tenant;
import com.smartlogix.domain.entity.User;
import com.smartlogix.domain.enums.UserRole;
import com.smartlogix.domain.repository.TenantRepository;
import com.smartlogix.domain.repository.UserRepository;
import com.smartlogix.dto.AuthRequest;
import com.smartlogix.dto.AuthResponse;
import com.smartlogix.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    /**
     * Authenticates a user by verifying the supplied credentials against the stored BCrypt hash.
     * <p>
     * On success, a signed JWT is generated containing the user's email (as the subject),
     * {@code tenantId}, and {@code role} claims. The token can subsequently be supplied as a
     * {@code Bearer} token in the {@code Authorization} header of every protected API call.
     * </p>
     *
     * @param request the login payload containing {@code email} and {@code password}
     * @return an {@link AuthResponse} holding the JWT token together with user metadata
     * @throws org.springframework.security.authentication.BadCredentialsException if the email
     *         is not found or the password does not match
     */
    @Transactional(readOnly = true)
    public AuthResponse login(AuthRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        String token = jwtUtil.generateToken(
                user.getEmail(),
                user.getTenant().getId(),
                user.getRole().name()
        );

        log.info("User logged in: email={}, tenantId={}", user.getEmail(), user.getTenant().getId());

        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .tenantId(user.getTenant().getId())
                .role(user.getRole().name())
                .build();
    }

    /**
     * Registers a new user within the specified tenant, creating the tenant if it does not exist.
     * <p>
     * The method first looks up (or creates) a {@link com.smartlogix.domain.entity.Tenant} by
     * {@code tenantSlug}. It then checks that no existing user with the same email already
     * belongs to that tenant. The user's password is BCrypt-encoded before persistence. On
     * success a JWT token is generated and returned, so the caller is immediately authenticated.
     * </p>
     *
     * @param email       the new user's email address (used as the login identifier)
     * @param password    the plain-text password (will be BCrypt-encoded before storage)
     * @param tenantSlug  the URL-friendly identifier for the tenant; a new tenant is created
     *                    automatically if one does not already exist with this slug
     * @return an {@link AuthResponse} containing the JWT token and user metadata
     * @throws IllegalStateException if a user with the supplied email already exists for the tenant
     */
    @Transactional
    public AuthResponse register(String email, String password, String tenantSlug) {
        Tenant tenant = tenantRepository.findBySlug(tenantSlug)
                .orElseGet(() -> {
                    Tenant newTenant = Tenant.builder()
                            .name(tenantSlug)
                            .slug(tenantSlug)
                            .active(true)
                            .build();
                    return tenantRepository.save(newTenant);
                });

        if (userRepository.findByEmailAndTenantId(email, tenant.getId()).isPresent()) {
            throw new IllegalStateException("User already exists with email: " + email);
        }

        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .role(UserRole.ROLE_USER)
                .tenant(tenant)
                .build();

        User saved = userRepository.save(user);
        log.info("Registered new user: email={}, tenantId={}", saved.getEmail(), tenant.getId());

        String token = jwtUtil.generateToken(saved.getEmail(), tenant.getId(), saved.getRole().name());

        return AuthResponse.builder()
                .token(token)
                .email(saved.getEmail())
                .tenantId(tenant.getId())
                .role(saved.getRole().name())
                .build();
    }
}
