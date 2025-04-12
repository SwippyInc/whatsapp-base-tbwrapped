package com.whatsapp.client.conversation.repository;

import com.whatsapp.client.conversation.model.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for WhatsApp conversations
 */
@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {
    
    /**
     * Find conversations for a tenant, ordered by most recent message
     * 
     * @param tenantId The tenant ID
     * @param pageable Pagination parameters
     * @return Page of conversations
     */
    Page<Conversation> findByTenantIdOrderByLastMessageAtDesc(UUID tenantId, Pageable pageable);
    
    /**
     * Find a conversation by tenant ID and customer WhatsApp ID
     * 
     * @param tenantId The tenant ID
     * @param customerWaId The customer's WhatsApp ID
     * @return Optional conversation
     */
    Optional<Conversation> findByTenantIdAndCustomerWaId(UUID tenantId, String customerWaId);
    
    /**
     * Find a conversation by tenant ID and customer phone number
     * 
     * @param tenantId The tenant ID
     * @param customerPhone The customer's phone number
     * @return Optional conversation
     */
    Optional<Conversation> findByTenantIdAndCustomerPhone(UUID tenantId, String customerPhone);
}