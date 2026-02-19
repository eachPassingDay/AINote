package com.ainote;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@org.springframework.scheduling.annotation.EnableAsync
public class AINoteApplication {

    public static void main(String[] args) {
        SpringApplication.run(AINoteApplication.class, args);
    }

    @org.springframework.context.annotation.Bean
    public org.springframework.boot.CommandLineRunner checkAiConnection(
            org.springframework.ai.chat.model.ChatModel chatModel) {
        return args -> {
            System.out.println("Checking AI connection...");
            try {
                String response = chatModel.call("Hello! Are you working?");
                System.out.println("AI Response: " + response);
                System.out.println("AI connection successful!");
            } catch (Exception e) {
                System.err.println("AI connection failed: " + e.getMessage());
                e.printStackTrace();
            }
        };
    }

}
