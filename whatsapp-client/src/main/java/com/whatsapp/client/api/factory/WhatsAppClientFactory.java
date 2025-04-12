package com.whatsapp.client.api.factory;

import com.whatsapp.api.WhatsappApiFactory;
import com.whatsapp.api.configuration.ApiVersion;
import com.whatsapp.api.impl.WhatsappBusinessCloudApi;
import com.whatsapp.api.impl.WhatsappBusinessManagementApi;
import com.whatsapp.client.tenant.model.WhatsAppTenant;
import com.whatsapp.client.tenant.repository.WhatsAppTenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory for creating and caching WhatsApp API clients for tenants
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WhatsAppClientFactory {
    
    private final WhatsAppTenantRepository tenantRepository;
    
    // Cache of API instances by tenant ID
    private final Map<UUID, WhatsappBusinessCloudApi> apiInstanceCache = new ConcurrentHashMap<>();
    
    /**
     * Get a WhatsApp business cloud API client for a tenant
     * 
     * @param tenantId The tenant ID
     * @return WhatsappBusinessCloudApi instance for the tenant
     * @throws IllegalArgumentException if the tenant is not found or not connected
     */
    public WhatsappBusinessCloudApi getClientForTenant(UUID tenantId) {
        WhatsAppTenant tenant = tenantRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("WhatsApp tenant not found for ID: " + tenantId));
        
        if (!tenant.isConnected()) {
            throw new IllegalArgumentException("WhatsApp is not connected for tenant: " + tenantId);
        }
        
        // Return cached instance if it exists and token is not expired
        if (apiInstanceCache.containsKey(tenantId) && !tenant.isTokenExpired()) {
            return apiInstanceCache.get(tenantId);
        }
        
        // Create a new instance
        WhatsappBusinessCloudApi api = WhatsappApiFactory.newInstance(tenant.getAccessToken()).newBusinessCloudApi();
        
        // Cache the instance
        apiInstanceCache.put(tenantId, api);
        
        return api;
    }
    
    /**
     * Get a WhatsApp business cloud API client for a tenant with specific API version
     * 
     * @param tenantId The tenant ID
     * @param apiVersion The API version to use
     * @return WhatsappBusinessCloudApi instance for the tenant
     */
    public WhatsappBusinessCloudApi getClientForTenant(UUID tenantId, ApiVersion apiVersion) {
        WhatsAppTenant tenant = tenantRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("WhatsApp tenant not found for ID: " + tenantId));
        
        if (!tenant.isConnected()) {
            throw new IllegalArgumentException("WhatsApp is not connected for tenant: " + tenantId);
        }
        
        // Create a new instance with specific version (not caching these to avoid complexity)
        return WhatsappApiFactory.newInstance(tenant.getAccessToken()).newBusinessCloudApi(apiVersion);
    }
    
    /**
     * Get a WhatsApp business management API client for a tenant
     * 
     * @param tenantId The tenant ID
     * @return WhatsappBusinessManagementApi instance for the tenant
     */
    public WhatsappBusinessManagementApi getManagementClientForTenant(UUID tenantId) {
        WhatsAppTenant tenant = tenantRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("WhatsApp tenant not found for ID: " + tenantId));
        
        if (!tenant.isConnected()) {
            throw new IllegalArgumentException("WhatsApp is not connected for tenant: " + tenantId);
        }
        
        return WhatsappApiFactory.newInstance(tenant.getAccessToken()).newBusinessManagementApi();
    }
    
    /**
     * Create a client with a direct access token (for initialization and OAuth flows)
     * 
     * @param accessToken The access token to use
     * @return WhatsappBusinessCloudApi instance
     */
    public WhatsappBusinessCloudApi createClientWithToken(String accessToken) {
        return WhatsappApiFactory.newInstance(accessToken).newBusinessCloudApi();
    }
    
    /**
     * Invalidate the cached API instance for a tenant
     * 
     * @param tenantId The tenant ID
     */
    public void invalidateClient(UUID tenantId) {
        apiInstanceCache.remove(tenantId);
    }
}