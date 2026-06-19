package com.yaqazah.user.service;

import com.yaqazah.company.model.Company;
import com.yaqazah.company.repository.CompanyRepository;
import com.yaqazah.company.service.CompanyService;
import com.yaqazah.infrastructure.email.NotificationService;
import com.yaqazah.user.dto.CompanyAdminDto;
import com.yaqazah.user.dto.CompanyOwnerRegistrationDto;
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
public class AdminService {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService; // Added this
    private final CompanyService companyService;


@Transactional
public void addCompanyAdmin(CompanyAdminDto req, String adminEmail) {
    if (userRepository.findByEmail(req.getEmail()).isPresent()) {
        throw new IllegalArgumentException("Email is already taken!");
    }

    User loggedInAdmin = userRepository.findByEmail(adminEmail)
            .orElseThrow(() -> new IllegalStateException("Admin user not found."));

    User newAdmin = new User();
    newAdmin.setEmail(req.getEmail());
    newAdmin.setFullName(req.getFullName());
    newAdmin.setRole(Role.COMPANY_ADMIN);

    // Link admin to the same company as the logged-in Admin
    newAdmin.setCompany(loggedInAdmin.getCompany());
    newAdmin.setPasswordHash(passwordEncoder.encode(req.getPassword()));
    newAdmin.setStatus(UserStatus.ACTIVE);

    userRepository.save(newAdmin);

    notificationService.sendWelcomePasswordSetEmail(newAdmin.getEmail());
}

    @Transactional
    public void registerCompanyOwner(CompanyOwnerRegistrationDto req) {
        // 1. Validate email first
        if (userRepository.findByEmail(req.getAdminEmail()).isPresent()) {
            throw new IllegalArgumentException("Email is already taken!");
        }

        // 2. Create the Company
        Company newCompany = new Company();
        newCompany.setName(req.getCompanyName());
        newCompany.setAddress(req.getCompanyAddress());

        Company savedCompany = companyService.createCompany(newCompany);

        User newAdmin = new User();
        newAdmin.setEmail(req.getAdminEmail());
        newAdmin.setFullName(req.getAdminFullName());
        newAdmin.setRole(Role.COMPANY_ADMIN);
        newAdmin.setCompany(savedCompany); // Link the saved company here!
        newAdmin.setPasswordHash(passwordEncoder.encode(req.getAdminPassword()));
        newAdmin.setStatus(UserStatus.ACTIVE);

        // 4. Save to DB and send email
        userRepository.save(newAdmin);
    }
//    @Transactional
//    public void addCompanyAdmin(UUID companyId, User newAdmin) {
//        if (userRepository.findByEmail(newAdmin.getEmail()).isPresent()) {
//            throw new IllegalArgumentException("Email is already taken!");
//        }
//
//        Company company = companyRepository.findById(companyId)
//                .orElseThrow(() -> new IllegalArgumentException("Company not found."));
//
//        newAdmin.setRole(Role.COMPANY_ADMIN);
//        newAdmin.setCompany(company);
//        newAdmin.setPasswordHash(passwordEncoder.encode(newAdmin.getPasswordHash()));
//        newAdmin.setStatus(UserStatus.ACTIVE);
//
////        newAdmin.setStatus(UserStatus.PENDING_VERIFICATION);
//
//        userRepository.save(newAdmin);
//
//        notificationService.sendWelcomePasswordSetEmail(newAdmin.getEmail());
//    }
}
