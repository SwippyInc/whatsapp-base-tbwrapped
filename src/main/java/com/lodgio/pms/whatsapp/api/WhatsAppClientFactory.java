package com.lodgio.pms.whatsapp.api;

import com.lodgio.pms.whatsapp.tenant.model.WhatsAppTenant;
import com.lodgio.pms.whatsapp.tenant.repository.WhatsAppTenantRepository;
import com.whatsapp.api.WhatsappApiFactory;
import com.whatsapp.api.impl.WhatsappBusinessCloudApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory for creating and caching WhatsappBusinessCloudApi instances for tenants
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WhatsAppClientFactory {
    
    private final WhatsAppTenantRepository tenantRepository;
    
    // Cache of API instances by tenant ID
    private final Map<UUID, WhatsappBusinessCloudApi> apiInstanceCache = new ConcurrentHashMap<>();
    
    /**
     * Get a WhatsApp API client for a tenant
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
     * Invalidate the cached API instance for a tenant
     * 
     * @param tenantId The tenant ID
     */
    public void invalidateClient(UUID tenantId) {
        apiInstanceCache.remove(tenantId);
    }
}