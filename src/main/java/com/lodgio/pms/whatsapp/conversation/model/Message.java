package com.lodgio.pms.whatsapp.conversation.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Represents a WhatsApp message in a conversation
 */
@Entity
@Table(name = "messages", schema = "whatsapp_integration")
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
        INBOUND,
        OUTBOUND
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
        UNKNOWN
    }
    
    public enum MessageStatus {
        SENT,
        DELIVERED,
        READ,
        FAILED
    }
}