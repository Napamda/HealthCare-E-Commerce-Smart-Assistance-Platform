package org.example.Healthcareplatform.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Create/update request for a user address. recipientName, phone,
 * street and city are required (address validation rule).
 */
@Data
public class AddressRequest {

    @Size(max = 50, message = "Label must be at most 50 characters")
    private String label;

    @NotBlank(message = "Recipient name is required")
    @Size(max = 100, message = "Recipient name must be at most 100 characters")
    private String recipientName;

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^\\+?[0-9 ()\\-]{7,20}$", message = "Phone must be 7-20 digits, optionally starting with +")
    private String phone;

    @NotBlank(message = "Street is required")
    @Size(max = 255, message = "Street must be at most 255 characters")
    private String street;

    @NotBlank(message = "City is required")
    @Size(max = 100, message = "City must be at most 100 characters")
    private String city;

    @Size(max = 100, message = "State must be at most 100 characters")
    private String state;

    @Pattern(regexp = "^[0-9A-Za-z \\-]{0,20}$", message = "Postal code must be up to 20 letters/digits")
    private String postalCode;

    @Size(max = 100, message = "Country must be at most 100 characters")
    private String country;

    private Boolean isDefault;
}
