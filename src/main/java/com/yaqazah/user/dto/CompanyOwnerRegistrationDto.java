package com.yaqazah.user.dto;

import com.yaqazah.user.model.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyOwnerRegistrationDto {
    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    private String adminEmail;

    @NotBlank(message = "Full name is required")
    private String adminFullName;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    @Pattern(
            regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{8,}$",
            message = "Password must contain at least one number, one lowercase, and one uppercase letter"
    )
    private String adminPassword;

    @NotNull(message = "Gender is required")
    private Gender adminGender;

    @NotBlank(message = "Company name is required")
    private String companyName;
    private String companyAddress;
}