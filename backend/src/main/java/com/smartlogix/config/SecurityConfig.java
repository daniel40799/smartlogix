package com.smartlogix.config;

import com.smartlogix.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Central Spring Security configuration for SmartLogix.
 * <p>
 * Configures a stateless, JWT-based security model:
 * <ul>
 *   <li>CSRF protection is disabled — the API relies on bearer tokens in the
 *       {@code Authorization} header, not on cookies, so CSRF attacks are not applicable.</li>
 *   <li>Session management is set to {@link SessionCreationPolicy#STATELESS}; no HTTP session
 *       is ever created or used.</li>
 *   <li>{@link com.smartlogix.security.JwtAuthFilter} is inserted before
 *       {@link UsernamePasswordAuthenticationFilter} to extract and validate JWT tokens on
 *       every request.</li>
 *   <li>Method-level security ({@code @PreAuthorize}) is enabled via
 *       {@code @EnableMethodSecurity}.</li>
 * </ul>
 * </p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    /** JWT authentication filter that validates Bearer tokens on incoming requests. */
    private final JwtAuthFilter jwtAuthFilter;

    /** Spring Security user details service backed by the {@code users} database table. */
    private final UserDetailsService userDetailsService;

    /**
     * Defines the HTTP security filter chain applied to every request.
     * <p>
     * Public paths (auth, Swagger UI, Actuator, WebSocket) are permitted without
     * authentication; all other paths require a valid JWT.
     * </p>
     *
     * @param http the {@link HttpSecurity} builder provided by Spring Security
     * @return the fully configured {@link SecurityFilterChain}
     * @throws Exception if an error occurs during filter chain construction
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF disabled: API uses stateless JWT in Authorization header (not cookies),
                // so CSRF attacks do not apply to this application.
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/actuator/**",
                                "/ws/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Configures a {@link DaoAuthenticationProvider} that loads users from the database and
     * verifies passwords using BCrypt.
     *
     * @return the configured {@link AuthenticationProvider}
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * Provides the {@link PasswordEncoder} bean used throughout the application to encode
     * and verify passwords. Uses BCrypt with the default strength (10 rounds).
     *
     * @return a {@link BCryptPasswordEncoder} instance
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Exposes the {@link AuthenticationManager} as a Spring bean so it can be injected
     * into service classes that need to programmatically authenticate users.
     *
     * @param config the auto-configured {@link AuthenticationConfiguration}
     * @return the application's {@link AuthenticationManager}
     * @throws Exception if the manager cannot be retrieved from the configuration
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
