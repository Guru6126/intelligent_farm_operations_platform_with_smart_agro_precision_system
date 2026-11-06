package com.agroprecision.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Explicitly defines an {@link ObjectMapper} bean.
 * Spring Boot normally auto‑configures this, but declaring it
 * guarantees that any component that injects {@code ObjectMapper}
 * (e.g. {@code TableRepository}) will find a bean.
 */
@Configuration
public class JacksonConfig {
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}

