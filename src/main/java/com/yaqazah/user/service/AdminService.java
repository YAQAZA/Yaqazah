package com.yaqazah.user.service;

import com.yaqazah.common.util.PasswordGeneratorUtil;
import com.yaqazah.company.model.Company;
import com.yaqazah.company.service.CompanyService;
import com.yaqazah.infrastructure.email.NotificationService;
import com.yaqazah.user.dto.CompanyAdminDto;
import com.yaqazah.user.dto.CompanyOwnerRegistrationDto;
import com.yaqazah.user.dto.FleetDriverDto;
import com.yaqazah.user.model.Role;
import com.yaqazah.user.model.User;
import com.yaqazah.user.model.UserStatus;
import com.yaqazah.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;
    private final CompanyService companyService;

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

        // 3. Map DTO to User
        User newAdmin = new User();
        newAdmin.setEmail(req.getAdminEmail());
        newAdmin.setFullName(req.getAdminFullName());
        newAdmin.setGender(req.getAdminGender()); // Already validated as non-null by DTO
        newAdmin.setRole(Role.COMPANY_ADMIN);
        newAdmin.setCompany(savedCompany);
        newAdmin.setPasswordHash(passwordEncoder.encode(req.getAdminPassword()));
        newAdmin.setStatus(UserStatus.ACTIVE);
        newAdmin.setBirthDate(req.getBirthDate());

        // 4. Save to DB
        userRepository.save(newAdmin);
    }

    @Transactional
    public void addCompanyAdmin(CompanyAdminDto req, String adminEmail) {
        if (userRepository.findByEmail(req.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email is already taken!");
        }

        User loggedInAdmin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new IllegalStateException("Admin user not found."));

        // Map DTO to User
        User newCompanyAdmin = new User();
        newCompanyAdmin.setEmail(req.getEmail());
        newCompanyAdmin.setFullName(req.getFullName());
        newCompanyAdmin.setGender(req.getGender());
        newCompanyAdmin.setRole(Role.COMPANY_ADMIN);
        newCompanyAdmin.setCompany(loggedInAdmin.getCompany()); // Link to same company
        newCompanyAdmin.setBirthDate(req.getBirthDate());

        // Auto-generate password
        String rawTempPassword = PasswordGeneratorUtil.generateCompliantPassword();
        newCompanyAdmin.setPasswordHash(passwordEncoder.encode(rawTempPassword));

        newCompanyAdmin.setStatus(UserStatus.ACTIVE);

        userRepository.save(newCompanyAdmin);
        notificationService.sendWelcomePasswordSetEmail(newCompanyAdmin.getEmail());
    }

    @Transactional
    public void addFleetDriver(FleetDriverDto req, String adminEmail) {
        if (userRepository.findByEmail(req.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email is already taken!");
        }

        User loggedInAdmin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new IllegalStateException("Admin user not found."));

        // Map DTO to User
        User newDriver = new User();
        newDriver.setEmail(req.getEmail());
        newDriver.setFullName(req.getFullName());
        newDriver.setGender(req.getGender());
        newDriver.setRole(Role.FLEET_DRIVER);
        newDriver.setCompany(loggedInAdmin.getCompany());
        newDriver.setBirthDate(req.getBirthDate());

        String rawTempPassword = PasswordGeneratorUtil.generateCompliantPassword();
        newDriver.setPasswordHash(passwordEncoder.encode(rawTempPassword));

        newDriver.setStatus(UserStatus.ACTIVE);

        userRepository.save(newDriver);
        notificationService.sendWelcomePasswordSetEmail(newDriver.getEmail());
    }
}