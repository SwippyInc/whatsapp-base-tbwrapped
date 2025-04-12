package com.whatsapp.client.conversation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request for sending a text message
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageRequest {
    
    @NotBlank(message = "Recipient phone number is required")
    private String recipientPhone;
    
    @NotBlank(message = "Message text is required")
    private String text;
}