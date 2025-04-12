package com.lodgio.pms.whatsapp.tenant.repository;

import com.lodgio.pms.whatsapp.tenant.model.WhatsAppTenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WhatsAppTenantRepository extends JpaRepository<WhatsAppTenant, UUID> {
    
    Optional<WhatsAppTenant> findByTenantId(UUID tenantId);
    
    Optional<WhatsAppTenant> findByWabaId(String wabaId);
    
    Optional<WhatsAppTenant> findByBusinessPhoneNumberId(String businessPhoneNumberId);
    
    Optional<WhatsAppTenant> findByPhoneNumber(String phoneNumber);
    
    boolean existsByTenantId(UUID tenantId);
}