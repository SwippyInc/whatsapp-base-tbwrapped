package com.whatsapp.client.conversation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.whatsapp.api.domain.messages.*;
import com.whatsapp.api.domain.messages.response.MessageResponse;
import com.whatsapp.client.api.factory.WhatsAppClientFactory;
import com.whatsapp.client.common.WhatsAppConstants.Request;
import com.whatsapp.client.conversation.dto.MessageRequest;
import com.whatsapp.client.conversation.service.ConversationService;
import com.whatsapp.client.tenant.model.WhatsAppTenant;
import com.whatsapp.client.tenant.service.WhatsAppTenantService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.Map;
import java.util.UUID;

/**
 * Controller for sending WhatsApp messages
 */
@RestController
@RequestMapping("/api/whatsapp/message")
@CrossOrigin(origins = "*", maxAge = 3600)
@Slf4j
public class MessageController {
    
    private final WhatsAppTenantService tenantService;
    private final WhatsAppClientFactory clientFactory;
    private final ConversationService conversationService;
    private final ObjectMapper objectMapper;
    
    @Autowired
    public MessageController(
            WhatsAppTenantService tenantService,
            WhatsAppClientFactory clientFactory,
            ConversationService conversationService,
            @Qualifier("whatsAppObjectMapper") ObjectMapper objectMapper) {
        this.tenantService = tenantService;
        this.clientFactory = clientFactory;
        this.conversationService = conversationService;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Send a text message
     * 
     * @param tenantId The tenant ID
     * @param request The message request
     * @return Message response with ID
     */
    @PostMapping("/{tenantId}/text")
    public ResponseEntity<Object> sendTextMessage(
            @PathVariable UUID tenantId,
            @RequestBody MessageRequest request) {
        
        log.info("Sending text message for tenant: {} to recipient: {}", tenantId, request.getRecipientPhone());
        
        try {
            // Check if tenant is connected
            if (!tenantService.isConnected(tenantId)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of(
                                Request.SUCCESS, false,
                                "error", "WhatsApp is not connected for this tenant"
                        ));
            }
            
            // Get tenant
            WhatsAppTenant tenant = tenantService.getTenant(tenantId);
            
            // Get WhatsApp client
            var whatsappClient = clientFactory.getClientForTenant(tenantId);
            
            // Create text message
            TextMessage textMessage = new TextMessage();
            textMessage.setBody(request.getText());
            
            // Build the message with MessageBuilder
            com.whatsapp.api.domain.messages.Message message = 
                com.whatsapp.api.domain.messages.Message.MessageBuilder.builder()
                .setTo(request.getRecipientPhone())
                .buildTextMessage(textMessage);
            
            // Send message
            MessageResponse response = whatsappClient.sendMessage(tenant.getBusinessPhoneNumberId(), message);
            
            // Track conversation (async)
            conversationService.trackOutboundMessage(tenantId, request.getRecipientPhone(), request.getText(), response);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error sending text message for tenant {}: {}", tenantId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            Request.SUCCESS, false,
                            "error", e.getMessage()
                    ));
        }
    }
    
    /**
     * Send a template message
     * 
     * @param tenantId The tenant ID
     * @param request The request body containing template details
     * @return Message response with ID
     */
    @PostMapping("/{tenantId}/template")
    public ResponseEntity<Object> sendTemplateMessage(
            @PathVariable UUID tenantId,
            @RequestBody Map<String, Object> request) {
        
        log.info("Sending template message for tenant: {}", tenantId);
        
        try {
            // Check if tenant is connected
            if (!tenantService.isConnected(tenantId)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of(
                                Request.SUCCESS, false,
                                "error", "WhatsApp is not connected for this tenant"
                        ));
            }
            
            // Get tenant
            WhatsAppTenant tenant = tenantService.getTenant(tenantId);
            
            // Get WhatsApp client
            var whatsappClient = clientFactory.getClientForTenant(tenantId);
            
            // First get "to" and "template" from the request map
            String to = (String) request.get("to");
            Map<String, Object> templateMap = (Map<String, Object>) request.get("template");
            String templateName = (String) templateMap.get("name");
            
            // Convert map to template message
            String jsonString = objectMapper.writeValueAsString(request);
            TemplateMessage templateMessage = objectMapper.readValue(jsonString, TemplateMessage.class);
            
            // Build full message
            com.whatsapp.api.domain.messages.Message message = 
                com.whatsapp.api.domain.messages.Message.MessageBuilder.builder()
                .setTo(to)
                .buildTemplateMessage(templateMessage);
            
            // Send message
            MessageResponse response = whatsappClient.sendMessage(tenant.getBusinessPhoneNumberId(), message);
            
            // Track conversation (basic tracking for template)
            conversationService.trackOutboundTemplateMessage(
                    tenantId, 
                    message.getTo(), 
                    "Template: " + templateName, 
                    response);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error sending template message for tenant {}: {}", tenantId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            Request.SUCCESS, false,
                            "error", e.getMessage()
                    ));
        }
    }
    
    /**
     * Send a media message (image, document, audio, video)
     * 
     * @param tenantId The tenant ID
     * @param mediaType The media type (image, document, audio, video)
     * @param request The request body containing media details
     * @return Message response with ID
     */
    @PostMapping("/{tenantId}/media/{mediaType}")
    public ResponseEntity<Object> sendMediaMessage(
            @PathVariable UUID tenantId,
            @PathVariable String mediaType,
            @RequestBody Map<String, Object> request) {
        
        log.info("Sending {} message for tenant: {}", mediaType, tenantId);
        
        try {
            // Check if tenant is connected
            if (!tenantService.isConnected(tenantId)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of(
                                Request.SUCCESS, false,
                                "error", "WhatsApp is not connected for this tenant"
                        ));
            }
            
            // Get tenant
            WhatsAppTenant tenant = tenantService.getTenant(tenantId);
            
            // Get WhatsApp client
            var whatsappClient = clientFactory.getClientForTenant(tenantId);
            
            // First get "to" from the request map
            String to = (String) request.get("to");
            
            // Convert request to appropriate message type
            String jsonString = objectMapper.writeValueAsString(request);
            com.whatsapp.api.domain.messages.Message message;
            
            switch (mediaType.toLowerCase()) {
                case "image":
                    ImageMessage imageMessage = objectMapper.readValue(jsonString, ImageMessage.class);
                    message = com.whatsapp.api.domain.messages.Message.MessageBuilder.builder()
                            .setTo(to)
                            .buildImageMessage(imageMessage);
                    break;
                case "document":
                    DocumentMessage documentMessage = objectMapper.readValue(jsonString, DocumentMessage.class);
                    message = com.whatsapp.api.domain.messages.Message.MessageBuilder.builder()
                            .setTo(to)
                            .buildDocumentMessage(documentMessage);
                    break;
                case "audio":
                    AudioMessage audioMessage = objectMapper.readValue(jsonString, AudioMessage.class);
                    message = com.whatsapp.api.domain.messages.Message.MessageBuilder.builder()
                            .setTo(to)
                            .buildAudioMessage(audioMessage);
                    break;
                case "video":
                    VideoMessage videoMessage = objectMapper.readValue(jsonString, VideoMessage.class);
                    message = com.whatsapp.api.domain.messages.Message.MessageBuilder.builder()
                            .setTo(to)
                            .buildVideoMessage(videoMessage);
                    break;
                default:
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of(
                                    Request.SUCCESS, false,
                                    "error", "Unsupported media type: " + mediaType
                            ));
            }
            
            // Send message
            MessageResponse response = whatsappClient.sendMessage(tenant.getBusinessPhoneNumberId(), message);
            
            // Track conversation
            conversationService.trackOutboundMediaMessage(
                    tenantId,
                    message.getTo(),
                    "Media: " + mediaType,
                    response);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error sending {} message for tenant {}: {}", mediaType, tenantId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            Request.SUCCESS, false,
                            "error", e.getMessage()
                    ));
        }
    }
    
    /**
     * Mark a message as read
     * 
     * @param tenantId The tenant ID
     * @param messageId The message ID to mark as read
     * @return Success response
     */
    @PostMapping("/{tenantId}/read/{messageId}")
    public ResponseEntity<Object> markMessageAsRead(
            @PathVariable UUID tenantId,
            @PathVariable String messageId) {
        
        log.info("Marking message as read for tenant: {}, message: {}", tenantId, messageId);
        
        try {
            // Check if tenant is connected
            if (!tenantService.isConnected(tenantId)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of(
                                Request.SUCCESS, false,
                                "error", "WhatsApp is not connected for this tenant"
                        ));
            }
            
            // Get tenant
            WhatsAppTenant tenant = tenantService.getTenant(tenantId);
            
            // Get WhatsApp client
            var whatsappClient = clientFactory.getClientForTenant(tenantId);
            
            // Create read message
            ReadMessage readMessage = new ReadMessage(messageId);
            
            // Send read receipt - this will need to be implemented in WhatsAppClientFactory 
            // since markMessageAsRead isn't a standard method in the API
            MessageResponse response = whatsappClient.sendMessage(tenant.getBusinessPhoneNumberId(), 
                    com.whatsapp.api.domain.messages.Message.MessageBuilder.builder()
                        .setTo(messageId) // temporary placeholder 
                        .buildReactionMessage(new ReactionMessage())); // temporary workaround
            
            // Update message status in our database
            conversationService.updateMessageReadStatus(messageId);
            
            return ResponseEntity.ok(Map.of(Request.SUCCESS, true));
            
        } catch (Exception e) {
            log.error("Error marking message as read for tenant {}: {}", tenantId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            Request.SUCCESS, false,
                            "error", e.getMessage()
                    ));
        }
    }
}