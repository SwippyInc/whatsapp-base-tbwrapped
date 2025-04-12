package com.whatsapp.client.tenant.repository;

import com.whatsapp.client.tenant.model.WhatsAppTenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for WhatsApp tenant management
 */
@Repository
public interface WhatsAppTenantRepository extends JpaRepository<WhatsAppTenant, UUID> {
    
    /**
     * Find a tenant by tenant ID
     * 
     * @param tenantId The tenant ID
     * @return Optional WhatsAppTenant
     */
    Optional<WhatsAppTenant> findByTenantId(UUID tenantId);
    
    /**
     * Find a tenant by WhatsApp Business Account ID
     * 
     * @param wabaId The WABA ID
     * @return Optional WhatsAppTenant
     */
    Optional<WhatsAppTenant> findByWabaId(String wabaId);
    
    /**
     * Find a tenant by business phone number ID
     * 
     * @param businessPhoneNumberId The phone number ID
     * @return Optional WhatsAppTenant
     */
    Optional<WhatsAppTenant> findByBusinessPhoneNumberId(String businessPhoneNumberId);
    
    /**
     * Find a tenant by phone number
     * 
     * @param phoneNumber The phone number
     * @return Optional WhatsAppTenant
     */
    Optional<WhatsAppTenant> findByPhoneNumber(String phoneNumber);
    
    /**
     * Check if a tenant exists by tenant ID
     * 
     * @param tenantId The tenant ID
     * @return true if exists
     */
    boolean existsByTenantId(UUID tenantId);
}