package com.whatsapp.client.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for WhatsApp integration
 */
@Configuration
@ConfigurationProperties(prefix = "whatsapp")
@Data
public class WhatsAppProperties {
    
    /**
     * Meta App ID for WhatsApp Business API
     */
    private String appId;
    
    /**
     * Meta App Secret for WhatsApp Business API
     */
    private String appSecret;
    
    /**
     * OAuth redirect URI for handling the callback
     */
    private String redirectUri;
    
    /**
     * Webhook URL for receiving WhatsApp notifications
     */
    private String webhookUrl;
    
    /**
     * Secret for verifying webhook requests
     */
    private String webhookVerifyToken;
    
    /**
     * Configuration ID for embedded signup
     */
    private String configurationId;
}