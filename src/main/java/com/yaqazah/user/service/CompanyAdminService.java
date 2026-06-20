package com.yaqazah.user.service;

import com.yaqazah.infrastructure.email.NotificationService;
import com.yaqazah.user.model.Gender;
import com.yaqazah.user.model.Role;
import com.yaqazah.user.model.User;
import com.yaqazah.user.model.UserStatus;
import com.yaqazah.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@NullMarked
public class CompanyAdminService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;

    @Transactional
    public void addFleetDriver(User newDriver, String adminEmail) {
        if (userRepository.findByEmail(newDriver.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email is already taken!");
        }

        User loggedInAdmin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new IllegalStateException("Admin user not found."));

        // Set business rules
        newDriver.setRole(Role.FLEET_DRIVER);
        // Link driver to the same company as the Admin
        newDriver.setCompany(loggedInAdmin.getCompany());
        newDriver.setPasswordHash(passwordEncoder.encode(RandomStringUtils.secure().nextAlphanumeric(8)));
        newDriver.setStatus(UserStatus.ACTIVE);
//        newDriver.setStatus(UserStatus.PENDING_VERIFICATION);
        newDriver.setFullName(newDriver.getFullName() != null ? newDriver.getFullName() : "Unnamed Driver");
        newDriver.setEmail(newDriver.getEmail());
        newDriver.setGender(newDriver.getGender() != null ? newDriver.getGender() : Gender.MALE);

        userRepository.save(newDriver);

        notificationService.sendWelcomePasswordSetEmail(newDriver.getEmail());
    }

    @Transactional
    public void updateFleetDriver(UUID driverId, User updatedData, String adminEmail) {
        User loggedInAdmin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new IllegalStateException("Admin user not found."));

        User existingDriver = userRepository.findById(driverId)
                .orElseThrow(() -> new IllegalArgumentException("Driver not found."));

        // Security Check: Is this a fleet driver?
        if (existingDriver.getRole() != Role.FLEET_DRIVER) {
            throw new IllegalArgumentException("Target user is not a fleet driver.");
        }

        // Security Check: Does this driver belong to the Admin's company?
        if (existingDriver.getCompany() == null ||
                !existingDriver.getCompany().getCompanyId().equals(loggedInAdmin.getCompany().getCompanyId())) {
            throw new IllegalStateException("You are not authorized to edit drivers outside your company.");
        }

        // Apply updates
        if (updatedData.getFullName() != null) existingDriver.setFullName(updatedData.getFullName());
        if (updatedData.getGender() != null) existingDriver.setGender(updatedData.getGender());
        if (updatedData.getStatus() != null) existingDriver.setStatus(updatedData.getStatus());

        if (updatedData.getPasswordHash() != null && !updatedData.getPasswordHash().trim().isEmpty()) {
            existingDriver.setPasswordHash(passwordEncoder.encode(updatedData.getPasswordHash()));
        }

        userRepository.save(existingDriver);
    }
}