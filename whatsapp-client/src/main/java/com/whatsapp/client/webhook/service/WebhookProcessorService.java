package com.whatsapp.client.webhook.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whatsapp.client.common.WhatsAppConstants.Message;
import com.whatsapp.client.common.WhatsAppConstants.Webhook;
import com.whatsapp.client.conversation.service.ConversationService;
import com.whatsapp.client.tenant.repository.WhatsAppTenantRepository;
import com.whatsapp.client.tenant.service.WhatsAppTenantService;
import com.whatsapp.client.webhook.model.WebhookEvent;
import com.whatsapp.client.webhook.repository.WebhookEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service for processing WhatsApp webhook events
 */
@Service
@Slf4j
public class WebhookProcessorService {
    
    private final WebhookEventRepository webhookEventRepository;
    private final WhatsAppTenantRepository tenantRepository;
    private final WhatsAppTenantService tenantService;
    private final ConversationService conversationService;
    private final ObjectMapper objectMapper;
    
    @Autowired
    public WebhookProcessorService(
            WebhookEventRepository webhookEventRepository,
            WhatsAppTenantRepository tenantRepository,
            WhatsAppTenantService tenantService,
            ConversationService conversationService,
            @Qualifier("whatsAppObjectMapper") ObjectMapper objectMapper) {
        this.webhookEventRepository = webhookEventRepository;
        this.tenantRepository = tenantRepository;
        this.tenantService = tenantService;
        this.conversationService = conversationService;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Process an incoming webhook event
     * 
     * @param payload The webhook payload
     */
    @Async
    @Transactional
    public void processWebhook(JsonNode payload) {
        try {
            String object = payload.get(Webhook.OBJECT_TYPE).asText();
            
            // Only process WhatsApp Business Account webhooks
            if (!Webhook.OBJECT_WHATSAPP.equals(object)) {
                log.warn("Ignoring non-WhatsApp webhook: {}", object);
                return;
            }
            
            // Process each entry in the webhook
            for (JsonNode entry : payload.get(Webhook.ENTRY)) {
                processEntry(entry);
            }
            
        } catch (Exception e) {
            log.error("Error processing webhook: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Process an entry in the webhook payload
     * 
     * @param entry The entry node
     */
    private void processEntry(JsonNode entry) {
        try {
            String wabaId = entry.get(Webhook.ID).asText();
            
            // Find the tenant for this WABA ID
            var tenantOpt = tenantRepository.findByWabaId(wabaId);
            
            if (tenantOpt.isEmpty()) {
                log.warn("Received webhook for unknown WABA ID: {}", wabaId);
                // Store the event anyway for future processing
                storeWebhookEvent(null, entry);
                return;
            }
            
            var tenant = tenantOpt.get();
            UUID tenantId = tenant.getTenantId();
            
            // Store the event
            WebhookEvent event = storeWebhookEvent(tenantId, entry);
            
            // Process changes in the entry
            if (entry.has(Webhook.CHANGES)) {
                for (JsonNode change : entry.get(Webhook.CHANGES)) {
                    processChange(tenant.getTenantId(), change, event);
                }
            }
            
        } catch (Exception e) {
            log.error("Error processing webhook entry: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Process a change in a webhook entry
     * 
     * @param tenantId The tenant ID
     * @param change The change node
     * @param event The stored webhook event
     */
    private void processChange(UUID tenantId, JsonNode change, WebhookEvent event) {
        try {
            String field = change.get(Webhook.FIELD).asText();
            JsonNode value = change.get(Webhook.VALUE);
            
            switch (field) {
                case Webhook.FIELD_ACCOUNT_UPDATE:
                    processAccountUpdate(tenantId, value, event);
                    break;
                    
                case Webhook.FIELD_MESSAGES:
                    processMessages(tenantId, value, event);
                    break;
                    
                case Webhook.FIELD_TEMPLATE_STATUS:
                    processTemplateStatusUpdate(tenantId, value, event);
                    break;
                    
                default:
                    log.info("Unhandled webhook field type: {}", field);
                    break;
            }
            
            // Mark the event as processed
            event.markProcessed();
            webhookEventRepository.save(event);
            
        } catch (Exception e) {
            log.error("Error processing webhook change: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Process account update events
     * 
     * @param tenantId The tenant ID
     * @param value The value node
     * @param event The stored webhook event
     */
    private void processAccountUpdate(UUID tenantId, JsonNode value, WebhookEvent event) {
        try {
            String eventType = value.get(Webhook.EVENT).asText();
            
            switch (eventType) {
                case Webhook.EVENT_PARTNER_ADDED:
                    // This event indicates the WhatsApp Business Account (WABA) has been created
                    // and our app has been added as a partner
                    if (value.has(Webhook.WABA_INFO)) {
                        JsonNode wabaInfo = value.get(Webhook.WABA_INFO);
                        String wabaId = wabaInfo.get(Webhook.WABA_ID).asText();
                        
                        log.info("WABA created and partner added for tenant {}: {}", tenantId, wabaId);
                        
                        // We need to wait for phone_number events to get the phone number ID
                    }
                    break;
                    
                case Webhook.EVENT_ACCOUNT_UPDATE:
                    // General account update, could be various things
                    log.info("Account update for tenant {}: {}", tenantId, value);
                    break;
                    
                case Webhook.EVENT_ACCOUNT_VERIFIED:
                case Webhook.EVENT_VERIFIED_ACCOUNT:
                    // Account has been verified
                    log.info("Account verified for tenant {}", tenantId);
                    break;
                    
                case Webhook.EVENT_DISABLED_UPDATE:
                    // Account has been disabled
                    log.warn("Account disabled for tenant {}", tenantId);
                    break;
                    
                default:
                    log.info("Unhandled account update event: {}", eventType);
                    break;
            }
            
        } catch (Exception e) {
            log.error("Error processing account update: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Process message events
     * 
     * @param tenantId The tenant ID
     * @param value The value node
     * @param event The stored webhook event
     */
    private void processMessages(UUID tenantId, JsonNode value, WebhookEvent event) {
        try {
            // Check if this is an incoming message
            if (value.has(Message.MESSAGES)) {
                // Handle incoming messages
                for (JsonNode message : value.get(Message.MESSAGES)) {
                    String messageId = message.get(Message.ID).asText();
                    
                    // Forward to conversation service to handle the message
                    conversationService.handleIncomingMessage(tenantId, message, value);
                }
            }
            
            // Check if this is a message status update
            if (value.has(Message.STATUSES)) {
                // Handle status updates (delivered, read, etc.)
                for (JsonNode status : value.get(Message.STATUSES)) {
                    String messageId = status.get(Message.ID).asText();
                    String statusType = status.get(Message.STATUS).asText();
                    
                    // Forward to conversation service to update the message status
                    conversationService.updateMessageStatus(messageId, statusType);
                }
            }
            
        } catch (Exception e) {
            log.error("Error processing messages: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Process template status update events
     * 
     * @param tenantId The tenant ID
     * @param value The value node
     * @param event The stored webhook event
     */
    private void processTemplateStatusUpdate(UUID tenantId, JsonNode value, WebhookEvent event) {
        try {
            String eventType = value.get(Webhook.EVENT).asText();
            String templateId = value.get("message_template_id").asText();
            String templateName = value.get("message_template_name").asText();
            
            log.info("Template status update for tenant {}: {} - {} - {}", 
                    tenantId, templateName, templateId, eventType);
            
            // We could implement template management here if needed
            
        } catch (Exception e) {
            log.error("Error processing template status update: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Store a webhook event in the database
     * 
     * @param tenantId The tenant ID (can be null)
     * @param payload The webhook payload
     * @return The stored webhook event
     */
    private WebhookEvent storeWebhookEvent(UUID tenantId, JsonNode payload) {
        try {
            // Extract the event type
            String eventType = "UNKNOWN";
            
            if (payload.has(Webhook.CHANGES) && payload.get(Webhook.CHANGES).isArray() && 
                    payload.get(Webhook.CHANGES).size() > 0) {
                JsonNode firstChange = payload.get(Webhook.CHANGES).get(0);
                if (firstChange.has(Webhook.FIELD)) {
                    eventType = firstChange.get(Webhook.FIELD).asText();
                }
            }
            
            // Create and save the event
            WebhookEvent event = WebhookEvent.builder()
                    .tenantId(tenantId)
                    .eventType(eventType)
                    .payload(payload)
                    .processed(false)
                    .build();
            
            return webhookEventRepository.save(event);
            
        } catch (Exception e) {
            log.error("Error storing webhook event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to store webhook event", e);
        }
    }
}