package com.whatsapp.client.tenant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Response for WhatsApp connection status
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionStatusResponse {
    
    private UUID tenantId;
    private String status;
    private boolean connected;
    private String errorMessage;
}