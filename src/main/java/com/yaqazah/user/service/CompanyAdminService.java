package com.yaqazah.user.service;

import com.yaqazah.company.model.Company;
import com.yaqazah.company.repository.CompanyRepository;
import com.yaqazah.infrastructure.email.NotificationService;
import com.yaqazah.user.model.Role;
import com.yaqazah.user.model.User;
import com.yaqazah.user.model.UserStatus;
import com.yaqazah.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CompanyAdminService {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService; // Added this

    @Transactional
    public void addCompanyAdmin(UUID companyId, User newAdmin) {
        if (userRepository.findByEmail(newAdmin.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email is already taken!");
        }

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new IllegalArgumentException("Company not found."));

        newAdmin.setRole(Role.COMPANY_ADMIN);
        newAdmin.setCompany(company);
        newAdmin.setPasswordHash(passwordEncoder.encode(newAdmin.getPasswordHash()));
        newAdmin.setStatus(UserStatus.ACTIVE);

//        newAdmin.setStatus(UserStatus.PENDING_VERIFICATION);

        userRepository.save(newAdmin);

        notificationService.sendWelcomePasswordSetEmail(newAdmin.getEmail());
    }
}