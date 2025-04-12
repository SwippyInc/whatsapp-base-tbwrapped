package com.lodgio.pms.whatsapp.conversation.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Represents a WhatsApp conversation between a business and a customer
 */
@Entity
@Table(name = "conversations", schema = "whatsapp_integration")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Conversation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    
    @Column(name = "customer_wa_id", nullable = false)
    private String customerWaId;
    
    @Column(name = "customer_phone", nullable = false)
    private String customerPhone;
    
    @Column(name = "customer_name")
    private String customerName;
    
    @Column(name = "customer_profile_pic_url")
    private String customerProfilePicUrl;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private ConversationStatus status = ConversationStatus.ACTIVE;
    
    @Column(name = "last_message_at")
    private OffsetDateTime lastMessageAt;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
    
    public enum ConversationStatus {
        ACTIVE,
        ARCHIVED,
        BLOCKED
    }
}