package com.lodgio.pms.whatsapp.conversation.repository;

import com.lodgio.pms.whatsapp.conversation.model.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {
    
    Page<Conversation> findByTenantIdOrderByLastMessageAtDesc(UUID tenantId, Pageable pageable);
    
    Optional<Conversation> findByTenantIdAndCustomerWaId(UUID tenantId, String customerWaId);
    
    Optional<Conversation> findByTenantIdAndCustomerPhone(UUID tenantId, String customerPhone);
}