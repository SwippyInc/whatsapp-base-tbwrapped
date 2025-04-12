package com.whatsapp.client.tenant.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whatsapp.client.api.factory.WhatsAppClientFactory;
import com.whatsapp.client.common.WhatsAppConstants.Api;
import com.whatsapp.client.common.WhatsAppConstants.OAuth;
import com.whatsapp.client.common.WhatsAppConstants.Request;
import com.whatsapp.client.config.WhatsAppProperties;
import com.whatsapp.client.tenant.model.WhatsAppTenant;
import com.whatsapp.client.tenant.repository.WhatsAppTenantRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Service for managing WhatsApp tenants and connections through WhatsApp embedded signup flow
 */
@Service
@Slf4j
public class WhatsAppTenantService {
    
    private final WhatsAppTenantRepository tenantRepository;
    private final WhatsAppClientFactory clientFactory;
    private final WhatsAppProperties whatsAppProperties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    @Autowired
    public WhatsAppTenantService(
            WhatsAppTenantRepository tenantRepository,
            WhatsAppClientFactory clientFactory,
            WhatsAppProperties whatsAppProperties,
            @Qualifier("whatsAppRestTemplate") RestTemplate restTemplate,
            @Qualifier("whatsAppObjectMapper") ObjectMapper objectMapper) {
        this.tenantRepository = tenantRepository;
        this.clientFactory = clientFactory;
        this.whatsAppProperties = whatsAppProperties;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }
    
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
        
        // Generate state parameter for CSRF protection (includes tenant ID)
        String state = tenantId.toString();
        
        // Generate authorization URL based on embedded signup documentation
        return UriComponentsBuilder.fromHttpUrl(OAuth.OAUTH_AUTHORIZE_URL)
                .queryParam(OAuth.PARAM_CLIENT_ID, whatsAppProperties.getAppId())
                .queryParam(OAuth.PARAM_REDIRECT_URI, whatsAppProperties.getRedirectUri())
                .queryParam(OAuth.PARAM_STATE, state)
                .queryParam(OAuth.PARAM_SCOPE, OAuth.OAUTH_SCOPES)
                .build()
                .toUriString();
    }
    
    /**
     * Handle the OAuth callback from the WhatsApp embedded signup flow (Step 1 in onboarding)
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
            // Step 1: Exchange the code for an access token
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add(OAuth.PARAM_CLIENT_ID, whatsAppProperties.getAppId());
            params.add(OAuth.PARAM_CLIENT_SECRET, whatsAppProperties.getAppSecret());
            params.add(OAuth.PARAM_CODE, code);
            params.add(OAuth.PARAM_REDIRECT_URI, whatsAppProperties.getRedirectUri());
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            
            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(params, headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                    Api.OAUTH_TOKEN_ENDPOINT,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );
            
            // Parse the response
            JsonNode jsonResponse = objectMapper.readTree(response.getBody());
            
            String accessToken = jsonResponse.get(OAuth.RESPONSE_ACCESS_TOKEN).asText();
            String refreshToken = null;
            if (jsonResponse.has(OAuth.RESPONSE_REFRESH_TOKEN)) {
                refreshToken = jsonResponse.get(OAuth.RESPONSE_REFRESH_TOKEN).asText();
            }
            
            long expiresIn = jsonResponse.get(OAuth.RESPONSE_EXPIRES_IN).asLong();
            OffsetDateTime expiresAt = OffsetDateTime.now().plusSeconds(expiresIn);
            
            // Extract WhatsApp business account ID and phone number ID from the payload
            // This information should be in the WhatsApp session message event from the frontend
            // For now, we'll update the token information and rely on webhook events to get this information
            
            // Update the tenant with the token information
            tenant.setAccessToken(accessToken);
            tenant.setRefreshToken(refreshToken);
            tenant.setTokenExpiresAt(expiresAt);
            
            // The remaining steps (subscribing to webhooks, registering phone number) will be handled separately
            // after webhook events indicate the account creation is complete
            
            // Mark as needing verification (next steps need to be completed)
            tenant.setConnectionStatus(WhatsAppTenant.ConnectionStatus.VERIFICATION_NEEDED);
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
     * Complete the onboarding process after receiving webhook confirmation (Step 2-3 in onboarding)
     * 
     * @param tenantId The tenant ID
     * @param wabaId The WhatsApp Business Account ID
     * @param phoneNumberId The business phone number ID
     */
    @Transactional
    public void completeOnboarding(UUID tenantId, String wabaId, String phoneNumberId) {
        WhatsAppTenant tenant = tenantRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("WhatsApp tenant not found for ID: " + tenantId));
        
        if (tenant.getConnectionStatus() != WhatsAppTenant.ConnectionStatus.VERIFICATION_NEEDED) {
            throw new IllegalStateException("Tenant is not in the verification needed state: " + tenantId);
        }
        
        try {
            // Update tenant with WABA and phone number information
            tenant.setWabaId(wabaId);
            tenant.setBusinessPhoneNumberId(phoneNumberId);
            
            // Step 2: Subscribe to webhooks on the customer's WABA
            subscribeToWebhooks(tenant);
            
            // Step 3: Register the customer's phone number with a PIN
            // This step may be handled separately as it requires a user-generated PIN
            
            // Mark as connected
            tenant.setConnectionStatus(WhatsAppTenant.ConnectionStatus.CONNECTED);
            tenantRepository.save(tenant);
            
            // Invalidate any cached API instance
            clientFactory.invalidateClient(tenantId);
            
        } catch (Exception e) {
            log.error("Error completing WhatsApp onboarding for tenant {}: {}", tenantId, e.getMessage(), e);
            tenant.setConnectionStatus(WhatsAppTenant.ConnectionStatus.ERROR);
            tenantRepository.save(tenant);
            throw new RuntimeException("Failed to complete WhatsApp onboarding: " + e.getMessage(), e);
        }
    }
    
    /**
     * Register the phone number with a 2-step verification PIN
     * 
     * @param tenantId The tenant ID
     * @param pin The 6-digit PIN for 2-step verification
     */
    @Transactional
    public void registerPhoneNumber(UUID tenantId, String pin) {
        WhatsAppTenant tenant = tenantRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("WhatsApp tenant not found for ID: " + tenantId));
        
        if (tenant.getBusinessPhoneNumberId() == null) {
            throw new IllegalArgumentException("Phone number ID not found for tenant: " + tenantId);
        }
        
        try {
            // Create the request body
            String requestBody = String.format(
                    "{\"" + Request.MESSAGING_PRODUCT + "\":\"" + Request.MESSAGING_PRODUCT_WHATSAPP + "\",\"" + Request.PIN + "\":\"%s\"}",
                    pin
            );
            
            // Set up headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(tenant.getAccessToken());
            
            HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);
            
            // Make the request to register the phone number
            String registerEndpoint = String.format(
                    Api.GRAPH_API_URL + "/%s" + Request.REGISTER,
                    tenant.getBusinessPhoneNumberId()
            );
            
            ResponseEntity<String> response = restTemplate.exchange(
                    registerEndpoint,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );
            
            // Check the response
            if (response.getStatusCode() == HttpStatus.OK) {
                // If successful, update the tenant status
                tenant.setConnectionStatus(WhatsAppTenant.ConnectionStatus.CONNECTED);
                tenantRepository.save(tenant);
            } else {
                throw new RuntimeException("Failed to register phone number: " + response.getBody());
            }
            
        } catch (Exception e) {
            log.error("Error registering phone number for tenant {}: {}", tenantId, e.getMessage(), e);
            throw new RuntimeException("Failed to register phone number: " + e.getMessage(), e);
        }
    }
    
    /**
     * Subscribe to webhooks on the customer's WABA
     * 
     * @param tenant The WhatsApp tenant
     */
    private void subscribeToWebhooks(WhatsAppTenant tenant) {
        try {
            // Set up headers
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(tenant.getAccessToken());
            
            HttpEntity<String> requestEntity = new HttpEntity<>(headers);
            
            // Make the request to subscribe to webhooks
            String subscribeEndpoint = String.format(
                    Api.GRAPH_API_URL + "/%s" + Request.WABA_SUBSCRIBED_APPS,
                    tenant.getWabaId()
            );
            
            ResponseEntity<String> response = restTemplate.exchange(
                    subscribeEndpoint,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );
            
            // Check the response
            if (response.getStatusCode() != HttpStatus.OK) {
                throw new RuntimeException("Failed to subscribe to webhooks: " + response.getBody());
            }
            
        } catch (Exception e) {
            log.error("Error subscribing to webhooks for tenant {}: {}", tenant.getTenantId(), e.getMessage(), e);
            throw new RuntimeException("Failed to subscribe to webhooks: " + e.getMessage(), e);
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
        
        // Only attempt to unsubscribe if we have the necessary information
        if (tenant.isConnected() && tenant.getWabaId() != null) {
            try {
                // Set up headers
                HttpHeaders headers = new HttpHeaders();
                headers.setBearerAuth(tenant.getAccessToken());
                
                HttpEntity<String> requestEntity = new HttpEntity<>(headers);
                
                // Make the request to unsubscribe from webhooks
                String unsubscribeEndpoint = String.format(
                        Api.GRAPH_API_URL + "/%s" + Request.WABA_SUBSCRIBED_APPS,
                        tenant.getWabaId()
                );
                
                restTemplate.exchange(
                        unsubscribeEndpoint,
                        HttpMethod.DELETE,
                        requestEntity,
                        String.class
                );
                
            } catch (Exception e) {
                log.warn("Error unsubscribing from webhooks for tenant {}: {}", tenantId, e.getMessage());
                // Continue with disconnection even if unsubscribe fails
            }
        }
        
        // Update tenant status
        tenant.setConnectionStatus(WhatsAppTenant.ConnectionStatus.DISCONNECTED);
        tenant.setAccessToken(null);
        tenant.setRefreshToken(null);
        tenant.setTokenExpiresAt(null);
        
        tenantRepository.save(tenant);
        
        // Invalidate any cached API instance
        clientFactory.invalidateClient(tenantId);
    }
    
    /**
     * Refresh the access token for a tenant
     * 
     * @param tenantId The tenant ID
     * @return true if token was refreshed successfully
     */
    @Transactional
    public boolean refreshToken(UUID tenantId) {
        WhatsAppTenant tenant = tenantRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("WhatsApp tenant not found for ID: " + tenantId));
        
        if (tenant.getRefreshToken() == null) {
            log.warn("No refresh token available for tenant: {}", tenantId);
            return false;
        }
        
        try {
            // Set up the token refresh request
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add(OAuth.PARAM_GRANT_TYPE, OAuth.GRANT_TYPE_REFRESH_TOKEN);
            params.add(OAuth.PARAM_CLIENT_ID, whatsAppProperties.getAppId());
            params.add(OAuth.PARAM_CLIENT_SECRET, whatsAppProperties.getAppSecret());
            params.add(OAuth.PARAM_REFRESH_TOKEN, tenant.getRefreshToken());
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            
            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(params, headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                    Api.OAUTH_TOKEN_ENDPOINT,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );
            
            // Parse the response
            JsonNode jsonResponse = objectMapper.readTree(response.getBody());
            
            String accessToken = jsonResponse.get(OAuth.RESPONSE_ACCESS_TOKEN).asText();
            String refreshToken = null;
            if (jsonResponse.has(OAuth.RESPONSE_REFRESH_TOKEN)) {
                refreshToken = jsonResponse.get(OAuth.RESPONSE_REFRESH_TOKEN).asText();
            }
            
            long expiresIn = jsonResponse.get(OAuth.RESPONSE_EXPIRES_IN).asLong();
            OffsetDateTime expiresAt = OffsetDateTime.now().plusSeconds(expiresIn);
            
            // Update the tenant with the new token information
            tenant.setAccessToken(accessToken);
            if (refreshToken != null) {
                tenant.setRefreshToken(refreshToken);
            }
            tenant.setTokenExpiresAt(expiresAt);
            
            tenantRepository.save(tenant);
            
            // Invalidate any cached API instance
            clientFactory.invalidateClient(tenantId);
            
            return true;
            
        } catch (Exception e) {
            log.error("Error refreshing token for tenant {}: {}", tenantId, e.getMessage(), e);
            return false;
        }
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
    
    /**
     * Check if a tenant's WhatsApp is connected
     * 
     * @param tenantId The tenant ID
     * @return true if WhatsApp is connected
     */
    public boolean isConnected(UUID tenantId) {
        return tenantRepository.findByTenantId(tenantId)
                .map(WhatsAppTenant::isConnected)
                .orElse(false);
    }
    
    /**
     * Get tenant details by ID
     * 
     * @param tenantId The tenant ID
     * @return The WhatsApp tenant
     */
    public WhatsAppTenant getTenant(UUID tenantId) {
        return tenantRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("WhatsApp tenant not found for ID: " + tenantId));
    }
}