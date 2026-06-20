package com.yaqazah.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CompanyOwnerRegistrationDto {
    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    private String adminEmail;

    @NotBlank(message = "Full name is required")
    private String adminFullName;
    private String adminPassword;

    @NotBlank(message = "Full name is required")
    private String companyName;
    private String companyAddress;
}