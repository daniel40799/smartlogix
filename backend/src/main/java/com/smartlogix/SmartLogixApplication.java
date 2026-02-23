package com.smartlogix;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class SmartLogixApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartLogixApplication.class, args);
    }
}
