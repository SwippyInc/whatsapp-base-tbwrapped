package com.whatsapp.client.tenant.controller;

import com.whatsapp.client.common.WhatsAppConstants.Request;
import com.whatsapp.client.tenant.dto.ConnectionInitResponse;
import com.whatsapp.client.tenant.dto.ConnectionStatusResponse;
import com.whatsapp.client.tenant.dto.RegisterPinRequest;
import com.whatsapp.client.tenant.dto.WhatsAppTenantDto;
import com.whatsapp.client.tenant.model.WhatsAppTenant;
import com.whatsapp.client.tenant.service.WhatsAppTenantService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.cors.CorsConfiguration;

import java.util.Map;
import java.util.UUID;

/**
 * Controller for managing WhatsApp tenant connections
 */
@RestController
@RequestMapping("/api/whatsapp/tenant")
@CrossOrigin(origins = "*", maxAge = 3600)
@Slf4j
public class WhatsAppTenantController {
    
    private final WhatsAppTenantService tenantService;
    
    @Autowired
    public WhatsAppTenantController(WhatsAppTenantService tenantService) {
        this.tenantService = tenantService;
    }
    
    /**
     * Initialize a WhatsApp connection for a tenant
     * 
     * @param tenantId The tenant ID
     * @param businessName The business name
     * @return The authorization URL for the WhatsApp embedded signup flow
     */
    @PostMapping("/connect/{tenantId}")
    public ResponseEntity<ConnectionInitResponse> initializeConnection(
            @PathVariable UUID tenantId,
            @RequestParam(name = "businessName", required = true) String businessName) {
        
        log.info("Initializing WhatsApp connection for tenant: {}, business: {}", tenantId, businessName);
        
        try {
            String authUrl = tenantService.initializeConnection(tenantId, businessName);
            
            ConnectionInitResponse response = ConnectionInitResponse.builder()
                    .success(true)
                    .authorizationUrl(authUrl)
                    .message("WhatsApp connection initialized. Redirect user to the authorization URL.")
                    .build();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error initializing WhatsApp connection for tenant {}: {}", tenantId, e.getMessage(), e);
            
            ConnectionInitResponse response = ConnectionInitResponse.builder()
                    .success(false)
                    .message("Failed to initialize WhatsApp connection: " + e.getMessage())
                    .build();
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Handle the OAuth callback from WhatsApp embedded signup flow
     * 
     * @param code The authorization code
     * @param state The state parameter (tenant ID)
     * @return A success response
     */
    @GetMapping("/oauth/callback")
    public ResponseEntity<String> handleOAuthCallback(
            @RequestParam("code") String code,
            @RequestParam("state") String state) {
        
        log.info("Received OAuth callback with state: {}", state);
        
        try {
            tenantService.handleOAuthCallback(code, state);
            return ResponseEntity.ok("WhatsApp connection in progress. You can now close this window.");
            
        } catch (Exception e) {
            log.error("Error handling OAuth callback: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to connect WhatsApp: " + e.getMessage());
        }
    }
    
    /**
     * Register a phone number with 2-step verification PIN
     * 
     * @param tenantId The tenant ID
     * @param request The PIN request
     * @return A success response
     */
    @PostMapping("/{tenantId}/register-pin")
    public ResponseEntity<Object> registerPhoneNumber(
            @PathVariable UUID tenantId,
            @RequestBody RegisterPinRequest request) {
        
        log.info("Registering phone number with PIN for tenant: {}", tenantId);
        
        try {
            tenantService.registerPhoneNumber(tenantId, request.getPin());
            return ResponseEntity.ok(Map.of(Request.SUCCESS, true));
            
        } catch (Exception e) {
            log.error("Error registering phone number for tenant {}: {}", tenantId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            Request.SUCCESS, false,
                            "error", e.getMessage()
                    ));
        }
    }
    
    /**
     * Complete onboarding with WABA ID and phone number ID (called from webhook processor)
     * 
     * @param tenantId The tenant ID
     * @param wabaId The WhatsApp Business Account ID
     * @param phoneNumberId The business phone number ID
     * @return A success response
     */
    @PostMapping("/{tenantId}/complete-onboarding")
    public ResponseEntity<Object> completeOnboarding(
            @PathVariable UUID tenantId,
            @RequestParam("wabaId") String wabaId,
            @RequestParam("phoneNumberId") String phoneNumberId) {
        
        log.info("Completing onboarding for tenant: {}, WABA: {}, phone: {}", 
                tenantId, wabaId, phoneNumberId);
        
        try {
            tenantService.completeOnboarding(tenantId, wabaId, phoneNumberId);
            return ResponseEntity.ok(Map.of(Request.SUCCESS, true));
            
        } catch (Exception e) {
            log.error("Error completing onboarding for tenant {}: {}", tenantId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            Request.SUCCESS, false,
                            "error", e.getMessage()
                    ));
        }
    }
    
    /**
     * Disconnect WhatsApp for a tenant
     * 
     * @param tenantId The tenant ID
     * @return A success response
     */
    @PostMapping("/{tenantId}/disconnect")
    public ResponseEntity<Object> disconnectWhatsApp(@PathVariable UUID tenantId) {
        log.info("Disconnecting WhatsApp for tenant: {}", tenantId);
        
        try {
            tenantService.disconnectWhatsApp(tenantId);
            return ResponseEntity.ok(Map.of(Request.SUCCESS, true));
            
        } catch (Exception e) {
            log.error("Error disconnecting WhatsApp for tenant {}: {}", tenantId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            Request.SUCCESS, false,
                            "error", e.getMessage()
                    ));
        }
    }
    
    /**
     * Get connection status for a tenant
     * 
     * @param tenantId The tenant ID
     * @return The connection status
     */
    @GetMapping("/{tenantId}/status")
    public ResponseEntity<ConnectionStatusResponse> getConnectionStatus(@PathVariable UUID tenantId) {
        log.info("Getting WhatsApp connection status for tenant: {}", tenantId);
        
        try {
            WhatsAppTenant.ConnectionStatus status = tenantService.getConnectionStatus(tenantId);
            boolean connected = tenantService.isConnected(tenantId);
            
            ConnectionStatusResponse response = ConnectionStatusResponse.builder()
                    .tenantId(tenantId)
                    .status(status.toString())
                    .connected(connected)
                    .build();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error getting connection status for tenant {}: {}", tenantId, e.getMessage(), e);
            
            ConnectionStatusResponse response = ConnectionStatusResponse.builder()
                    .tenantId(tenantId)
                    .status(WhatsAppTenant.ConnectionStatus.ERROR.toString())
                    .connected(false)
                    .errorMessage(e.getMessage())
                    .build();
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Get tenant details
     * 
     * @param tenantId The tenant ID
     * @return The tenant details
     */
    @GetMapping("/{tenantId}")
    public ResponseEntity<WhatsAppTenantDto> getTenant(@PathVariable UUID tenantId) {
        log.info("Getting WhatsApp tenant details for: {}", tenantId);
        
        try {
            WhatsAppTenant tenant = tenantService.getTenant(tenantId);
            
            WhatsAppTenantDto tenantDto = WhatsAppTenantDto.builder()
                    .tenantId(tenant.getTenantId())
                    .businessName(tenant.getBusinessName())
                    .wabaId(tenant.getWabaId())
                    .phoneNumber(tenant.getPhoneNumber())
                    .connectionStatus(tenant.getConnectionStatus().toString())
                    .connected(tenant.isConnected())
                    .createdAt(tenant.getCreatedAt())
                    .updatedAt(tenant.getUpdatedAt())
                    .build();
            
            return ResponseEntity.ok(tenantDto);
            
        } catch (Exception e) {
            log.error("Error getting tenant details for tenant {}: {}", tenantId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}