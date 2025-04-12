package com.whatsapp.client.tenant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response for WhatsApp connection initialization
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionInitResponse {
    
    private boolean success;
    private String authorizationUrl;
    private String message;
}