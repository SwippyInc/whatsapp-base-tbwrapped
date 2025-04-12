package com.whatsapp.client.tenant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO for WhatsApp tenant details
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WhatsAppTenantDto {
    
    private UUID tenantId;
    private String businessName;
    private String wabaId;
    private String phoneNumber;
    private String connectionStatus;
    private boolean connected;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}