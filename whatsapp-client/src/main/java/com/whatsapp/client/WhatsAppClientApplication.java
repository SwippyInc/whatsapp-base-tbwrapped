package com.whatsapp.client;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Main application class for WhatsApp Client
 */
@SpringBootApplication
@EnableAsync
@EnableTransactionManagement
public class WhatsAppClientApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(WhatsAppClientApplication.class, args);
    }
}