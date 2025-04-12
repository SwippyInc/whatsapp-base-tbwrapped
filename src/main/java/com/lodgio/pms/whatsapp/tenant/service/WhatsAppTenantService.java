package com.lodgio.pms.whatsapp.tenant.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lodgio.pms.whatsapp.api.WhatsAppClientFactory;
import com.lodgio.pms.whatsapp.config.WhatsAppProperties;
import com.lodgio.pms.whatsapp.tenant.model.WhatsAppTenant;
import com.lodgio.pms.whatsapp.tenant.repository.WhatsAppTenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Service for managing WhatsApp tenants and connections
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WhatsAppTenantService {
    
    private final WhatsAppTenantRepository tenantRepository;
    private final WhatsAppClientFactory clientFactory;
    private final WhatsAppProperties whatsAppProperties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    /**
     * Initialize a WhatsApp connection for a tenant
     * 
     * @param tenantId The tenant ID
     * @param businessName The business name
     * @return The authorization URL for the WhatsApp embedded signup flow
     */
    @Transactional
    public String initializeConnection(UUID tenantId, String businessName) {
        // Check if tenant already exists
        if (tenantRepository.existsByTenantId(tenantId)) {
            throw new IllegalArgumentException("WhatsApp connection already exists for tenant: " + tenantId);
        }
        
        // Create a new tenant record
        WhatsAppTenant tenant = WhatsAppTenant.builder()
                .tenantId(tenantId)
                .businessName(businessName)
                .connectionStatus(WhatsAppTenant.ConnectionStatus.DISCONNECTED)
                .build();
        
        tenantRepository.save(tenant);
        
        // Generate state parameter for CSRF protection
        String state = tenantId.toString();
        
        // Generate authorization URL
        return UriComponentsBuilder.fromHttpUrl("https://www.facebook.com/dialog/oauth")
                .queryParam("client_id", whatsAppProperties.getAppId())
                .queryParam("redirect_uri", whatsAppProperties.getRedirectUri())
                .queryParam("state", state)
                .queryParam("scope", "whatsapp_business_management,whatsapp_business_messaging")
                .build()
                .toUriString();
    }
    
    /**
     * Handle the OAuth callback from the WhatsApp embedded signup flow
     * 
     * @param code The authorization code
     * @param state The state parameter (contains tenant ID)
     */
    @Transactional
    public void handleOAuthCallback(String code, String state) {
        UUID tenantId = UUID.fromString(state);
        
        // Find the tenant
        WhatsAppTenant tenant = tenantRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("WhatsApp tenant not found for ID: " + tenantId));
        
        // Update tenant status
        tenant.setConnectionStatus(WhatsAppTenant.ConnectionStatus.CONNECTING);
        tenantRepository.save(tenant);
        
        try {
            // Exchange the code for an access token
            String tokenEndpoint = "https://graph.facebook.com/v21.0/oauth/access_token";
            
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("client_id", whatsAppProperties.getAppId());
            params.add("client_secret", whatsAppProperties.getAppSecret());
            params.add("code", code);
            params.add("redirect_uri", whatsAppProperties.getRedirectUri());
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            
            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(params, headers);
            
            String response = restTemplate.exchange(
                    tokenEndpoint,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            ).getBody();
            
            // Parse the response
            JsonNode jsonResponse = objectMapper.readTree(response);
            
            String accessToken = jsonResponse.get("access_token").asText();
            String refreshToken = null;
            if (jsonResponse.has("refresh_token")) {
                refreshToken = jsonResponse.get("refresh_token").asText();
            }
            
            long expiresIn = jsonResponse.get("expires_in").asLong();
            OffsetDateTime expiresAt = OffsetDateTime.now().plusSeconds(expiresIn);
            
            // Update the tenant with the token information
            tenant.setAccessToken(accessToken);
            tenant.setRefreshToken(refreshToken);
            tenant.setTokenExpiresAt(expiresAt);
            
            // TODO: Subscribe to webhooks and register phone number in a separate async task
            
            // For now, mark as connected (in reality, you would check the webhook subscription response)
            tenant.setConnectionStatus(WhatsAppTenant.ConnectionStatus.CONNECTED);
            tenantRepository.save(tenant);
            
            // Invalidate any cached API instance
            clientFactory.invalidateClient(tenantId);
            
        } catch (Exception e) {
            log.error("Error handling WhatsApp OAuth callback for tenant {}: {}", tenantId, e.getMessage(), e);
            tenant.setConnectionStatus(WhatsAppTenant.ConnectionStatus.ERROR);
            tenantRepository.save(tenant);
            throw new RuntimeException("Failed to connect WhatsApp: " + e.getMessage(), e);
        }
    }
    
    /**
     * Disconnect WhatsApp for a tenant
     * 
     * @param tenantId The tenant ID
     */
    @Transactional
    public void disconnectWhatsApp(UUID tenantId) {
        WhatsAppTenant tenant = tenantRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("WhatsApp tenant not found for ID: " + tenantId));
        
        // TODO: Unsubscribe from webhooks
        
        tenant.setConnectionStatus(WhatsAppTenant.ConnectionStatus.DISCONNECTED);
        tenant.setAccessToken(null);
        tenant.setRefreshToken(null);
        tenant.setTokenExpiresAt(null);
        
        tenantRepository.save(tenant);
        
        // Invalidate any cached API instance
        clientFactory.invalidateClient(tenantId);
    }
    
    /**
     * Get WhatsApp connection status for a tenant
     * 
     * @param tenantId The tenant ID
     * @return The connection status
     */
    public WhatsAppTenant.ConnectionStatus getConnectionStatus(UUID tenantId) {
        return tenantRepository.findByTenantId(tenantId)
                .map(WhatsAppTenant::getConnectionStatus)
                .orElse(WhatsAppTenant.ConnectionStatus.DISCONNECTED);
    }
}