package com.ainote;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@lombok.extern.slf4j.Slf4j
@SpringBootApplication
@org.springframework.scheduling.annotation.EnableAsync
@org.springframework.cache.annotation.EnableCaching
@org.springframework.retry.annotation.EnableRetry
public class AINoteApplication {

    public static void main(String[] args) {
        SpringApplication.run(AINoteApplication.class, args);
    }

    @org.springframework.context.annotation.Bean
    public org.springframework.boot.CommandLineRunner checkAiConnection(
            org.springframework.ai.chat.model.ChatModel chatModel) {
        return args -> {
            log.info("Checking AI connection...");
            try {
                String response = chatModel.call("Hello! Are you working?");
                log.info("AI Response: {}", response);
                log.info("AI connection successful!");
            } catch (Exception e) {
                log.error("AI connection failed: {}", e.getMessage(), e);
            }
        };
    }

}
