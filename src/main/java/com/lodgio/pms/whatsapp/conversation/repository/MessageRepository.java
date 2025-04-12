package com.lodgio.pms.whatsapp.conversation.repository;

import com.lodgio.pms.whatsapp.conversation.model.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {
    
    Page<Message> findByConversationIdOrderBySentAtDesc(UUID conversationId, Pageable pageable);
    
    Optional<Message> findByWhatsappMessageId(String whatsappMessageId);
}