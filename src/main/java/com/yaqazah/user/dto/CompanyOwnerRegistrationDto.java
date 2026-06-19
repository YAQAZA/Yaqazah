package com.yaqazah.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CompanyOwnerRegistrationDto {
    // Owner/Admin details
    private String adminEmail;
    private String adminFullName;
    private String adminPassword;

    // Company details
    private String companyName;
    private String companyAddress;
}