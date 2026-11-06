package com.agroprecision;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the combined AgroPrecision + FarmVerse Chatbot application.
 * Scans both the backend package (com.agroprecision) and the chatbot package
 * (com.farmverse.farmverse_chatbot) so that all @RestController, @Service, etc.
 * beans are registered in the same Spring context.
 */
@SpringBootApplication(scanBasePackages = {"com.agroprecision", "com.farmverse.farmverse_chatbot"})
public class AgroPrecisionApplication {
    public static void main(String[] args) {
        SpringApplication.run(AgroPrecisionApplication.class, args);
    }
}
