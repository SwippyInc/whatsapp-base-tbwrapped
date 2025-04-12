package com.whatsapp.client.webhook.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whatsapp.client.common.WhatsAppConstants.Webhook;
import com.whatsapp.client.config.WhatsAppProperties;
import com.whatsapp.client.webhook.service.WebhookProcessorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.CrossOrigin;

/**
 * Controller for handling WhatsApp webhook events
 */
@RestController
@RequestMapping("/api/webhook")
@CrossOrigin(origins = "*", maxAge = 3600)
@Slf4j
public class WebhookController {
    
    private final WhatsAppProperties whatsAppProperties;
    private final WebhookProcessorService webhookProcessorService;
    private final ObjectMapper objectMapper;
    
    @Autowired
    public WebhookController(
            WhatsAppProperties whatsAppProperties,
            WebhookProcessorService webhookProcessorService,
            @Qualifier("whatsAppObjectMapper") ObjectMapper objectMapper) {
        this.whatsAppProperties = whatsAppProperties;
        this.webhookProcessorService = webhookProcessorService;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Verify webhook endpoint for WhatsApp webhook verification
     * 
     * @param mode The hub.mode parameter
     * @param verifyToken The hub.verify_token parameter
     * @param challenge The hub.challenge parameter
     * @return The challenge string if verification is successful
     */
    @GetMapping
    public ResponseEntity<String> verifyWebhook(
            @RequestParam(Webhook.VERIFY_MODE) String mode,
            @RequestParam(Webhook.VERIFY_TOKEN) String verifyToken,
            @RequestParam(Webhook.VERIFY_CHALLENGE) String challenge) {
        
        log.info("Received webhook verification request: mode={}, token={}", mode, verifyToken);
        
        // Check if the mode and token are correct
        if (Webhook.VERIFY_MODE_SUBSCRIBE.equals(mode) && whatsAppProperties.getWebhookVerifyToken().equals(verifyToken)) {
            log.info("Webhook verified successfully");
            return ResponseEntity.ok(challenge);
        }
        
        log.warn("Webhook verification failed: Invalid mode or token");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    
    /**
     * Handle incoming webhook events from WhatsApp
     * 
     * @param payload The webhook payload
     * @return Acknowledgment response
     */
    @PostMapping
    public ResponseEntity<String> handleWebhook(@RequestBody String payload) {
        log.debug("Received webhook event: {}", payload);
        
        try {
            // Parse the payload
            JsonNode jsonPayload = objectMapper.readTree(payload);
            
            // Process the webhook event asynchronously
            webhookProcessorService.processWebhook(jsonPayload);
            
            // Always return 200 OK to acknowledge receipt
            return ResponseEntity.ok(Webhook.RESPONSE_SUCCESS);
            
        } catch (Exception e) {
            log.error("Error processing webhook: {}", e.getMessage(), e);
            
            // Still return 200 OK to avoid WhatsApp retrying
            return ResponseEntity.ok(Webhook.RESPONSE_SUCCESS);
        }
    }
}