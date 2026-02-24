package com.smartlogix;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Entry point for the SmartLogix Spring Boot application.
 * <p>
 * {@code @SpringBootApplication} enables component scanning, auto-configuration, and
 * property support for the entire {@code com.smartlogix} package tree.
 * {@code @EnableJpaAuditing} activates Spring Data JPA auditing so that
 * {@code @CreatedDate} and {@code @LastModifiedDate} fields on entities are
 * populated automatically.
 * </p>
 */
@SpringBootApplication
@EnableJpaAuditing
public class SmartLogixApplication {

    /**
     * Application entry point — bootstraps the Spring context and starts the embedded
     * Tomcat server.
     *
     * @param args command-line arguments forwarded to {@link SpringApplication#run}
     */
    public static void main(String[] args) {
        SpringApplication.run(SmartLogixApplication.class, args);
    }
}
