package com.whatsapp.client.conversation.repository;

import com.whatsapp.client.conversation.model.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for WhatsApp messages
 */
@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {
    
    /**
     * Find messages for a conversation, ordered by sent time (newest first)
     * 
     * @param conversationId The conversation ID
     * @param pageable Pagination parameters
     * @return Page of messages
     */
    Page<Message> findByConversationIdOrderBySentAtDesc(UUID conversationId, Pageable pageable);
    
    /**
     * Find a message by its WhatsApp message ID
     * 
     * @param whatsappMessageId The WhatsApp message ID
     * @return Optional message
     */
    Optional<Message> findByWhatsappMessageId(String whatsappMessageId);
    
    /**
     * Find recent messages for a conversation (newest first)
     * 
     * @param conversationId The conversation ID
     * @param limit Maximum number of messages to return
     * @return List of messages
     */
    List<Message> findTop20ByConversationIdOrderBySentAtDesc(UUID conversationId);
}