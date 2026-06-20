package com.yaqazah.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CompanyAdminDto {
    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    private String email;

//    @NotBlank(message = "Password is required")
////    @Size(min = 6, message = "Password must be at least 6 characters")
//    private String password;

    @NotBlank(message = "Full name is required")
    private String fullName;
}