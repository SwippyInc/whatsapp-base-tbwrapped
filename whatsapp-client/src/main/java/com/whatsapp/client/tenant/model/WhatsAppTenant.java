package com.whatsapp.client.tenant.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Represents a business customer (tenant) with WhatsApp Business account integration
 */
@Entity
@Table(name = "tenants", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WhatsAppTenant {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @Column(name = "tenant_id", nullable = false, unique = true)
    private UUID tenantId;
    
    @Column(name = "business_name", nullable = false)
    private String businessName;
    
    @Column(name = "waba_id", unique = true)
    private String wabaId;
    
    @Column(name = "business_phone_number_id")
    private String businessPhoneNumberId;
    
    @Column(name = "phone_number", unique = true)
    private String phoneNumber;
    
    @JsonIgnore
    @Column(name = "access_token", columnDefinition = "TEXT")
    private String accessToken;
    
    @JsonIgnore
    @Column(name = "refresh_token", columnDefinition = "TEXT")
    private String refreshToken;
    
    @Column(name = "token_expires_at")
    private OffsetDateTime tokenExpiresAt;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "connection_status", nullable = false)
    @Builder.Default
    private ConnectionStatus connectionStatus = ConnectionStatus.DISCONNECTED;
    
    @JsonIgnore
    @Column(name = "webhook_secret")
    private String webhookSecret;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
    
    public enum ConnectionStatus {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        VERIFICATION_NEEDED,
        ERROR
    }
    
    /**
     * Check if WhatsApp is connected
     * 
     * @return true if status is CONNECTED
     */
    public boolean isConnected() {
        return connectionStatus == ConnectionStatus.CONNECTED;
    }
    
    /**
     * Check if the access token is expired
     * 
     * @return true if token is expired
     */
    public boolean isTokenExpired() {
        return tokenExpiresAt != null && tokenExpiresAt.isBefore(OffsetDateTime.now());
    }
}