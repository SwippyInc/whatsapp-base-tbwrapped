package com.whatsapp.client.webhook.repository;

import com.whatsapp.client.webhook.model.WebhookEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for WhatsApp webhook events
 */
@Repository
public interface WebhookEventRepository extends JpaRepository<WebhookEvent, UUID> {
    
    /**
     * Find unprocessed webhook events
     * 
     * @param processed Processing status (false for unprocessed)
     * @return List of webhook events
     */
    List<WebhookEvent> findByProcessedOrderByCreatedAtAsc(boolean processed);
    
    /**
     * Find webhook events for a tenant
     * 
     * @param tenantId The tenant ID
     * @param pageable Pagination parameters
     * @return Page of webhook events
     */
    Page<WebhookEvent> findByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);
    
    /**
     * Find webhook events by type
     * 
     * @param eventType The event type
     * @param pageable Pagination parameters
     * @return Page of webhook events
     */
    Page<WebhookEvent> findByEventTypeOrderByCreatedAtDesc(String eventType, Pageable pageable);
}