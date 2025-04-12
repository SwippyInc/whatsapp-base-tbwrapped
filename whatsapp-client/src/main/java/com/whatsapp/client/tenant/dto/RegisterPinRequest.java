package com.whatsapp.client.tenant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request for registering phone number with 2-step verification PIN
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterPinRequest {
    
    @NotBlank(message = "PIN is required")
    @Pattern(regexp = "^\\d{6}$", message = "PIN must be a 6-digit number")
    private String pin;
}