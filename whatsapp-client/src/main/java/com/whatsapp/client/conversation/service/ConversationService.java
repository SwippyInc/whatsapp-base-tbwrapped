package com.whatsapp.client.conversation.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whatsapp.api.domain.messages.TextMessage;
import com.whatsapp.api.domain.messages.response.MessageResponse;
import com.whatsapp.api.impl.WhatsappBusinessCloudApi;
import com.whatsapp.client.api.factory.WhatsAppClientFactory;
import com.whatsapp.client.common.WhatsAppConstants.Message;
import com.whatsapp.client.conversation.model.Conversation;
import com.whatsapp.client.conversation.model.Message.MessageDirection;
import com.whatsapp.client.conversation.model.Message.MessageStatus;
import com.whatsapp.client.conversation.model.Message.MessageType;
import com.whatsapp.client.conversation.repository.ConversationRepository;
import com.whatsapp.client.conversation.repository.MessageRepository;
import com.whatsapp.client.tenant.model.WhatsAppTenant;
import com.whatsapp.client.tenant.repository.WhatsAppTenantRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing WhatsApp conversations and messages
 */
@Service
@Slf4j
public class ConversationService {
    
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final WhatsAppTenantRepository tenantRepository;
    private final WhatsAppClientFactory clientFactory;
    private final ObjectMapper objectMapper;
    
    @Autowired
    public ConversationService(
            ConversationRepository conversationRepository,
            MessageRepository messageRepository,
            WhatsAppTenantRepository tenantRepository,
            WhatsAppClientFactory clientFactory,
            @Qualifier("whatsAppObjectMapper") ObjectMapper objectMapper) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.tenantRepository = tenantRepository;
        this.clientFactory = clientFactory;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Get conversations for a tenant
     * 
     * @param tenantId The tenant ID
     * @param pageable Pagination parameters
     * @return Page of conversations
     */
    public Page<Conversation> getConversations(UUID tenantId, Pageable pageable) {
        return conversationRepository.findByTenantIdOrderByLastMessageAtDesc(tenantId, pageable);
    }
    
    /**
     * Get messages for a conversation
     * 
     * @param conversationId The conversation ID
     * @param pageable Pagination parameters
     * @return Page of messages
     */
    public Page<com.whatsapp.client.conversation.model.Message> getMessages(UUID conversationId, Pageable pageable) {
        return messageRepository.findByConversationIdOrderBySentAtDesc(conversationId, pageable);
    }
    
    /**
     * Send a text message to a customer
     * 
     * @param tenantId The tenant ID
     * @param customerPhone The customer's phone number
     * @param text The message text
     * @return The sent message
     */
    @Transactional
    public com.whatsapp.client.conversation.model.Message sendTextMessage(UUID tenantId, String customerPhone, String text) {
        try {
            // Get the WhatsApp client for this tenant
            WhatsappBusinessCloudApi api = clientFactory.getClientForTenant(tenantId);
            
            // Find or create conversation
            Conversation conversation = findOrCreateConversation(tenantId, customerPhone);
            
            // Get the tenant for phone number ID
            WhatsAppTenant tenant = tenantRepository.findByTenantId(tenantId)
                    .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantId));
            
            if (tenant.getBusinessPhoneNumberId() == null) {
                throw new IllegalStateException("Phone number ID not found for tenant: " + tenantId);
            }
            
            // Create the text message object
            TextMessage textMessage = new TextMessage();
            textMessage.setBody(text);
            
            // Build the message with MessageBuilder
            com.whatsapp.api.domain.messages.Message message = 
                com.whatsapp.api.domain.messages.Message.MessageBuilder.builder()
                .setTo(customerPhone)
                .buildTextMessage(textMessage);
            
            // Send the message using the tenant's phone number ID
            var response = api.sendMessage(tenant.getBusinessPhoneNumberId(), message);
            
            // Create a message record
            com.whatsapp.client.conversation.model.Message dbMessage = com.whatsapp.client.conversation.model.Message.builder()
                    .conversationId(conversation.getId())
                    .whatsappMessageId(response.messages().get(0).id())
                    .direction(MessageDirection.OUTBOUND)
                    .messageType(MessageType.TEXT)
                    .content(text)
                    .status(MessageStatus.SENT)
                    .build();
            
            // Save the message
            dbMessage = messageRepository.save(dbMessage);
            
            // Update the conversation
            conversation.setLastMessageAt(OffsetDateTime.now());
            conversationRepository.save(conversation);
            
            return dbMessage;
            
        } catch (Exception e) {
            log.error("Error sending text message for tenant {}: {}", tenantId, e.getMessage(), e);
            throw new RuntimeException("Failed to send WhatsApp message: " + e.getMessage(), e);
        }
    }
    
    /**
     * Find or create a conversation for a customer
     * 
     * @param tenantId The tenant ID
     * @param customerPhone The customer's phone number
     * @return The conversation
     */
    private Conversation findOrCreateConversation(UUID tenantId, String customerPhone) {
        Optional<Conversation> existingConversation = conversationRepository.findByTenantIdAndCustomerPhone(
                tenantId, customerPhone);
        
        if (existingConversation.isPresent()) {
            return existingConversation.get();
        }
        
        // Create a new conversation
        Conversation conversation = Conversation.builder()
                .tenantId(tenantId)
                .customerPhone(customerPhone)
                .customerWaId(customerPhone) // This will be updated when we receive a message
                .status(Conversation.ConversationStatus.ACTIVE)
                .build();
        
        return conversationRepository.save(conversation);
    }
    
    /**
     * Handle an incoming message from a webhook
     * 
     * @param tenantId The tenant ID
     * @param messageNode The message JSON node
     * @param metadataNode The metadata JSON node (for contact info)
     */
    @Transactional
    public void handleIncomingMessage(UUID tenantId, JsonNode messageNode, JsonNode metadataNode) {
        try {
            String messageId = messageNode.get(Message.ID).asText();
            String messageType = messageNode.get(Message.TYPE).asText();
            
            // Check if we've already processed this message
            if (messageRepository.findByWhatsappMessageId(messageId).isPresent()) {
                log.info("Skipping already processed message: {}", messageId);
                return;
            }
            
            // Extract customer information
            JsonNode contacts = metadataNode.get(Message.CONTACTS);
            if (contacts == null || contacts.size() == 0) {
                log.warn("No contact information in message: {}", messageId);
                return;
            }
            
            JsonNode contact = contacts.get(0);
            String customerWaId = contact.get(Message.CONTACT_WAID).asText();
            String customerPhone = contact.path(Message.CONTACT_WAID).asText();
            String customerName = contact.path(Message.CONTACT_PROFILE).path(Message.CONTACT_NAME).asText(null);
            
            // Find or create conversation
            Optional<Conversation> conversationOpt = conversationRepository.findByTenantIdAndCustomerWaId(
                    tenantId, customerWaId);
            
            Conversation conversation;
            if (conversationOpt.isPresent()) {
                conversation = conversationOpt.get();
                
                // Update customer info if needed
                if (customerName != null && (conversation.getCustomerName() == null || 
                        !conversation.getCustomerName().equals(customerName))) {
                    conversation.setCustomerName(customerName);
                }
            } else {
                // Create new conversation
                conversation = Conversation.builder()
                        .tenantId(tenantId)
                        .customerWaId(customerWaId)
                        .customerPhone(customerPhone)
                        .customerName(customerName)
                        .status(Conversation.ConversationStatus.ACTIVE)
                        .build();
            }
            
            // Create message based on type
            com.whatsapp.client.conversation.model.Message message = createMessageFromWebhook(conversation.getId(), messageNode, messageType);
            
            // Save message
            message = messageRepository.save(message);
            
            // Update conversation
            conversation.setLastMessageAt(message.getSentAt());
            conversationRepository.save(conversation);
            
        } catch (Exception e) {
            log.error("Error handling incoming message for tenant {}: {}", tenantId, e.getMessage(), e);
        }
    }
    
    /**
     * Create a message object from webhook data
     * 
     * @param conversationId The conversation ID
     * @param messageNode The message JSON node
     * @param messageType The message type
     * @return The created message
     */
    private com.whatsapp.client.conversation.model.Message createMessageFromWebhook(UUID conversationId, JsonNode messageNode, String messageType) {
        String messageId = messageNode.get(Message.ID).asText();
        com.whatsapp.client.conversation.model.Message.MessageBuilder builder = com.whatsapp.client.conversation.model.Message.builder()
                .conversationId(conversationId)
                .whatsappMessageId(messageId)
                .direction(MessageDirection.INBOUND)
                .status(MessageStatus.DELIVERED) // Incoming messages are delivered by definition
                .statusUpdatedAt(OffsetDateTime.now());
        
        // Set the message type and content based on type
        switch (messageType.toLowerCase()) {
            case Message.TYPE_TEXT:
                JsonNode text = messageNode.get(Message.TYPE_TEXT);
                builder.messageType(MessageType.TEXT)
                        .content(text.get(Message.TEXT_BODY).asText());
                break;
                
            case Message.TYPE_IMAGE:
                JsonNode image = messageNode.get(Message.TYPE_IMAGE);
                builder.messageType(MessageType.IMAGE)
                        .mediaId(image.path(Message.MEDIA_ID).asText(null))
                        .mediaUrl(image.path(Message.MEDIA_LINK).asText(null))
                        .mediaMimeType(image.path(Message.MEDIA_MIMETYPE).asText(null))
                        .content(image.path(Message.MEDIA_CAPTION).asText(null));
                break;
                
            case Message.TYPE_AUDIO:
                JsonNode audio = messageNode.get(Message.TYPE_AUDIO);
                builder.messageType(MessageType.AUDIO)
                        .mediaId(audio.path(Message.MEDIA_ID).asText(null))
                        .mediaUrl(audio.path(Message.MEDIA_LINK).asText(null))
                        .mediaMimeType(audio.path(Message.MEDIA_MIMETYPE).asText(null));
                break;
                
            case Message.TYPE_VIDEO:
                JsonNode video = messageNode.get(Message.TYPE_VIDEO);
                builder.messageType(MessageType.VIDEO)
                        .mediaId(video.path(Message.MEDIA_ID).asText(null))
                        .mediaUrl(video.path(Message.MEDIA_LINK).asText(null))
                        .mediaMimeType(video.path(Message.MEDIA_MIMETYPE).asText(null))
                        .content(video.path(Message.MEDIA_CAPTION).asText(null));
                break;
                
            case Message.TYPE_DOCUMENT:
                JsonNode document = messageNode.get(Message.TYPE_DOCUMENT);
                builder.messageType(MessageType.DOCUMENT)
                        .mediaId(document.path(Message.MEDIA_ID).asText(null))
                        .mediaUrl(document.path(Message.MEDIA_LINK).asText(null))
                        .mediaMimeType(document.path(Message.MEDIA_MIMETYPE).asText(null))
                        .mediaFilename(document.path(Message.MEDIA_FILENAME).asText(null))
                        .content(document.path(Message.MEDIA_CAPTION).asText(null));
                break;
                
            case Message.TYPE_LOCATION:
                JsonNode location = messageNode.get(Message.TYPE_LOCATION);
                String locationContent = String.format(
                        "Latitude: %s, Longitude: %s, Name: %s, Address: %s",
                        location.path(Message.LOCATION_LATITUDE).asText("N/A"),
                        location.path(Message.LOCATION_LONGITUDE).asText("N/A"),
                        location.path(Message.LOCATION_NAME).asText("N/A"),
                        location.path(Message.LOCATION_ADDRESS).asText("N/A")
                );
                builder.messageType(MessageType.LOCATION)
                        .content(locationContent);
                break;
                
            case Message.TYPE_BUTTON:
                JsonNode button = messageNode.get(Message.TYPE_BUTTON);
                builder.messageType(MessageType.BUTTON)
                        .content(button.path(Message.TEXT_BODY).asText("Button clicked"));
                break;
                
            case Message.TYPE_INTERACTIVE:
                JsonNode interactive = messageNode.get(Message.TYPE_INTERACTIVE);
                String interactiveType = interactive.path(Message.INTERACTIVE_TYPE).asText();
                String interactiveContent;
                
                if (Message.INTERACTIVE_BUTTON_REPLY.equals(interactiveType)) {
                    interactiveContent = interactive.path(Message.INTERACTIVE_BUTTON_REPLY).path(Message.INTERACTIVE_TITLE).asText("Button reply");
                } else if (Message.INTERACTIVE_LIST_REPLY.equals(interactiveType)) {
                    interactiveContent = interactive.path(Message.INTERACTIVE_LIST_REPLY).path(Message.INTERACTIVE_TITLE).asText("List reply");
                } else {
                    interactiveContent = "Interactive message: " + interactiveType;
                }
                
                builder.messageType(MessageType.INTERACTIVE)
                        .content(interactiveContent);
                break;
                
            default:
                builder.messageType(MessageType.UNKNOWN)
                        .content("Unsupported message type: " + messageType);
                break;
        }
        
        return builder.build();
    }
    
    /**
     * Update a message status based on a webhook event
     * 
     * @param messageId The WhatsApp message ID
     * @param status The new status
     */
    @Transactional
    public void updateMessageStatus(String messageId, String status) {
        try {
            Optional<com.whatsapp.client.conversation.model.Message> messageOpt = messageRepository.findByWhatsappMessageId(messageId);
            
            if (messageOpt.isEmpty()) {
                log.warn("Received status update for unknown message: {}", messageId);
                return;
            }
            
            com.whatsapp.client.conversation.model.Message message = messageOpt.get();
            
            // Map WhatsApp status to our status enum
            MessageStatus newStatus;
            switch (status.toLowerCase()) {
                case Message.STATUS_SENT:
                    newStatus = MessageStatus.SENT;
                    break;
                case Message.STATUS_DELIVERED:
                    newStatus = MessageStatus.DELIVERED;
                    break;
                case Message.STATUS_READ:
                    newStatus = MessageStatus.READ;
                    break;
                case Message.STATUS_FAILED:
                    newStatus = MessageStatus.FAILED;
                    break;
                default:
                    log.warn("Unknown message status: {}", status);
                    return;
            }
            
            // Update the message status
            if (message.updateStatus(newStatus, OffsetDateTime.now())) {
                messageRepository.save(message);
                log.info("Updated message {} status to {}", messageId, newStatus);
            }
            
        } catch (Exception e) {
            log.error("Error updating message status: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Update message read status (used when marking a message as read)
     * 
     * @param messageId The WhatsApp message ID
     */
    @Transactional
    public void updateMessageReadStatus(String messageId) {
        updateMessageStatus(messageId, Message.STATUS_READ);
    }
    
    /**
     * Track an outbound text message
     * 
     * @param tenantId The tenant ID
     * @param recipientPhone The recipient phone number
     * @param text The message text
     * @param response The WhatsApp API response
     */
    @Transactional
    public void trackOutboundMessage(UUID tenantId, String recipientPhone, String text, MessageResponse response) {
        try {
            // Find or create conversation
            Conversation conversation = findOrCreateConversation(tenantId, recipientPhone);
            
            // Get message ID from response
            String messageId = response.messages().get(0).id();
            
            // Create a message record
            com.whatsapp.client.conversation.model.Message dbMessage = com.whatsapp.client.conversation.model.Message.builder()
                    .conversationId(conversation.getId())
                    .whatsappMessageId(messageId)
                    .direction(MessageDirection.OUTBOUND)
                    .messageType(MessageType.TEXT)
                    .content(text)
                    .status(MessageStatus.SENT)
                    .statusUpdatedAt(OffsetDateTime.now())
                    .build();
            
            // Save the message
            messageRepository.save(dbMessage);
            
            // Update the conversation
            conversation.setLastMessageAt(OffsetDateTime.now());
            conversationRepository.save(conversation);
            
        } catch (Exception e) {
            log.error("Error tracking outbound message for tenant {}: {}", tenantId, e.getMessage(), e);
        }
    }
    
    /**
     * Track an outbound template message
     * 
     * @param tenantId The tenant ID
     * @param recipientPhone The recipient phone number
     * @param templateInfo Information about the template
     * @param response The WhatsApp API response
     */
    @Transactional
    public void trackOutboundTemplateMessage(UUID tenantId, String recipientPhone, String templateInfo, MessageResponse response) {
        try {
            // Find or create conversation
            Conversation conversation = findOrCreateConversation(tenantId, recipientPhone);
            
            // Get message ID from response
            String messageId = response.messages().get(0).id();
            
            // Create a message record
            com.whatsapp.client.conversation.model.Message dbMessage = com.whatsapp.client.conversation.model.Message.builder()
                    .conversationId(conversation.getId())
                    .whatsappMessageId(messageId)
                    .direction(MessageDirection.OUTBOUND)
                    .messageType(MessageType.TEMPLATE)
                    .content(templateInfo)
                    .status(MessageStatus.SENT)
                    .statusUpdatedAt(OffsetDateTime.now())
                    .build();
            
            // Save the message
            messageRepository.save(dbMessage);
            
            // Update the conversation
            conversation.setLastMessageAt(OffsetDateTime.now());
            conversationRepository.save(conversation);
            
        } catch (Exception e) {
            log.error("Error tracking outbound template message for tenant {}: {}", tenantId, e.getMessage(), e);
        }
    }
    
    /**
     * Track an outbound media message
     * 
     * @param tenantId The tenant ID
     * @param recipientPhone The recipient phone number
     * @param mediaInfo Information about the media
     * @param response The WhatsApp API response
     */
    @Transactional
    public void trackOutboundMediaMessage(UUID tenantId, String recipientPhone, String mediaInfo, MessageResponse response) {
        try {
            // Find or create conversation
            Conversation conversation = findOrCreateConversation(tenantId, recipientPhone);
            
            // Get message ID from response
            String messageId = response.messages().get(0).id();
            
            // Create a message record
            com.whatsapp.client.conversation.model.Message dbMessage = com.whatsapp.client.conversation.model.Message.builder()
                    .conversationId(conversation.getId())
                    .whatsappMessageId(messageId)
                    .direction(MessageDirection.OUTBOUND)
                    .messageType(MessageType.MEDIA)
                    .content(mediaInfo)
                    .status(MessageStatus.SENT)
                    .statusUpdatedAt(OffsetDateTime.now())
                    .build();
            
            // Save the message
            messageRepository.save(dbMessage);
            
            // Update the conversation
            conversation.setLastMessageAt(OffsetDateTime.now());
            conversationRepository.save(conversation);
            
        } catch (Exception e) {
            log.error("Error tracking outbound media message for tenant {}: {}", tenantId, e.getMessage(), e);
        }
    }
}