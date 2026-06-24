package com.yaqazah.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SwapOwnershipDto {
    @NotBlank(message = "Target user email is required.")
    @Email(message = "Must be a valid email format.")
    private String targetEmail;
}