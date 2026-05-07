package com.yaqazah.user.service;

import com.yaqazah.company.model.Company;
import com.yaqazah.company.repository.CompanyRepository;
import com.yaqazah.user.model.Role;
import com.yaqazah.user.model.User;
import com.yaqazah.user.model.UserStatus;
import com.yaqazah.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserService {
    @Autowired private UserRepository userRepository;
    @Autowired private CompanyRepository companyRepository; // Need this to fetch Company objects
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private AuthService authService;

    @Transactional
    public void addFleetDriver(User newDriver, String adminEmail) {
        if (userRepository.findByEmail(newDriver.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email is already taken!");
        }

        User loggedInAdmin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new IllegalStateException("Admin user not found."));

        newDriver.setRole(Role.FLEET_DRIVER);
        // CHANGE: Set the Company object instead of ID
        newDriver.setCompany(loggedInAdmin.getCompany());
        newDriver.setPasswordHash(passwordEncoder.encode(newDriver.getPasswordHash()));

        // Ensure they start as pending and get a verification email
        newDriver.setStatus(UserStatus.PENDING_VERIFICATION);
        userRepository.save(newDriver);

        authService.sendVerificationEmail(newDriver.getEmail());
    }

    @Transactional
    public void updateFleetDriver(UUID driverId, User updatedData, String adminEmail) {
        User loggedInAdmin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new IllegalStateException("Admin user not found."));

        User existingDriver = userRepository.findById(driverId)
                .orElseThrow(() -> new IllegalArgumentException("Driver not found."));

        if (existingDriver.getRole() != Role.FLEET_DRIVER) {
            throw new IllegalArgumentException("Target user is not a fleet driver.");
        }

        // CHANGE: Compare Company IDs through the relationship
        if (!existingDriver.getCompany().getCompanyId().equals(loggedInAdmin.getCompany().getCompanyId())) {
            throw new IllegalStateException("You are not authorized to edit drivers outside your company.");
        }

        if (updatedData.getFullName() != null) existingDriver.setFullName(updatedData.getFullName());
        if (updatedData.getGender() != null) existingDriver.setGender(updatedData.getGender());
        if (updatedData.getStatus() != null) existingDriver.setStatus(updatedData.getStatus());

        if (updatedData.getPasswordHash() != null && !updatedData.getPasswordHash().trim().isEmpty()) {
            existingDriver.setPasswordHash(passwordEncoder.encode(updatedData.getPasswordHash()));
        }

        userRepository.save(existingDriver);
    }

    @Transactional
    public void addCompanyAdmin(UUID companyId, User newAdmin) {
        if (userRepository.findByEmail(newAdmin.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email is already taken!");
        }

        // CHANGE: Fetch the actual Company entity
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new IllegalArgumentException("Company not found."));

        newAdmin.setRole(Role.COMPANY_ADMIN);
        newAdmin.setCompany(company);
        newAdmin.setPasswordHash(passwordEncoder.encode(newAdmin.getPasswordHash()));
        newAdmin.setStatus(UserStatus.PENDING_VERIFICATION);

        userRepository.save(newAdmin);
        authService.sendVerificationEmail(newAdmin.getEmail());
    }

    @Transactional
    public void deleteAccount(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        switch (user.getRole()) {
            case INDEPENDENT_DRIVER:
            case ADMIN:
                userRepository.delete(user);
                break;

            case COMPANY_ADMIN:
                // CHANGE: Use the company object to get the ID for the repository call
                UUID cid = user.getCompany().getCompanyId();
                int adminCount = userRepository.countByCompany_CompanyIdAndRole(cid, Role.COMPANY_ADMIN);

                if (adminCount <= 1) {
                    userRepository.deleteByCompany_CompanyIdAndRole(cid, Role.FLEET_DRIVER);
                }

                userRepository.delete(user);
                break;

            default:
                throw new IllegalStateException("Deletion not permitted for role: " + user.getRole());
        }
    }

    @Transactional
    public void updateUserName(String email, String newName) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        user.setFullName(newName);
        userRepository.save(user);
    }

    // Helper for the controller
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
    }
}