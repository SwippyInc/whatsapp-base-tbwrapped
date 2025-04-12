package com.whatsapp.client.conversation.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Represents a WhatsApp message in a conversation
 */
@Entity
@Table(name = "messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;
    
    @Column(name = "whatsapp_message_id")
    private String whatsappMessageId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false)
    private MessageDirection direction;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false)
    private MessageType messageType;
    
    @Column(name = "content")
    private String content;
    
    // Media fields
    @Column(name = "media_url")
    private String mediaUrl;
    
    @Column(name = "media_mime_type")
    private String mediaMimeType;
    
    @Column(name = "media_filename")
    private String mediaFilename;
    
    @Column(name = "media_id")
    private String mediaId;
    
    // Status fields
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private MessageStatus status = MessageStatus.SENT;
    
    @Column(name = "status_updated_at")
    private OffsetDateTime statusUpdatedAt;
    
    @CreationTimestamp
    @Column(name = "sent_at", updatable = false)
    private OffsetDateTime sentAt;
    
    @Column(name = "delivered_at")
    private OffsetDateTime deliveredAt;
    
    @Column(name = "read_at")
    private OffsetDateTime readAt;
    
    @Column(name = "failed_reason")
    private String failedReason;
    
    public enum MessageDirection {
        INBOUND,   // From customer to business
        OUTBOUND   // From business to customer
    }
    
    public enum MessageType {
        TEXT,
        IMAGE,
        AUDIO,
        VIDEO,
        DOCUMENT,
        LOCATION,
        CONTACT,
        INTERACTIVE,
        TEMPLATE,
        STICKER,
        REACTION,
        BUTTON,
        MEDIA,  // Generic media type for outbound media tracking
        UNKNOWN
    }
    
    public enum MessageStatus {
        SENT,       // Message sent to WhatsApp
        DELIVERED,  // Message delivered to recipient's device
        READ,       // Message read by recipient
        FAILED      // Message failed to send
    }
    
    /**
     * Update the message status if the new status is more advanced
     * 
     * @param newStatus The new status
     * @param timestamp The timestamp of the status update
     * @return true if status was updated
     */
    public boolean updateStatus(MessageStatus newStatus, OffsetDateTime timestamp) {
        if (shouldUpdateStatus(newStatus)) {
            this.status = newStatus;
            this.statusUpdatedAt = timestamp;
            
            switch (newStatus) {
                case DELIVERED -> this.deliveredAt = timestamp;
                case READ -> this.readAt = timestamp;
            }
            
            return true;
        }
        return false;
    }
    
    /**
     * Check if the status should be updated based on message flow:
     * SENT -> DELIVERED -> READ
     * 
     * @param newStatus The proposed new status
     * @return true if status should be updated
     */
    private boolean shouldUpdateStatus(MessageStatus newStatus) {
        if (newStatus == this.status) {
            return false;
        }
        
        return switch (this.status) {
            case SENT -> newStatus == MessageStatus.DELIVERED || newStatus == MessageStatus.READ;
            case DELIVERED -> newStatus == MessageStatus.READ;
            case READ, FAILED -> false;
        };
    }
}