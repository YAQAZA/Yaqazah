package com.yaqazah.user.service;

import com.yaqazah.user.model.Role;
import com.yaqazah.user.model.User;
import com.yaqazah.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

//    // General user retrieval
//    public User getUser(UUID id) {
//        return userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
//    }
//
//    // Handles logic for adding new users/drivers
//    public void addUser(User user) {
//        // Password hashing would happen here before saving
//        userRepository.save(user); // Matches Source
//    }
//
//    // Updates existing profile data
//    public void updateUser(User user) {
//        userRepository.save(user);
//    }
//
//    // Deletes user (Admin functionality)
//    public void deleteUser(UUID id) {
//        userRepository.deleteById(id);
//    }


    @Transactional
    public void addFleetDriver(User newDriver, String adminEmail) {
        // 1. Check if email exists
        if (userRepository.findByEmail(newDriver.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email is already taken!");
        }

        // 2. Fetch the logged-in admin to get their company ID
        User loggedInAdmin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new IllegalStateException("Admin user not found."));

        // 3. Apply business rules
        newDriver.setRole(Role.FLEET_DRIVER);
        newDriver.setCompanyId(loggedInAdmin.getCompanyId());
        newDriver.setPasswordHash(passwordEncoder.encode(newDriver.getPasswordHash()));

        // 4. Save to database
        userRepository.save(newDriver);
    }

    @Transactional
    public void addCompanyAdmin(UUID companyId, User newAdmin) {
        // 1. Check if email is already taken
        if (userRepository.findByEmail(newAdmin.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email is already taken!");
        }

        // 2. Force the role to COMPANY_ADMIN
        newAdmin.setRole(Role.COMPANY_ADMIN);

        // 3. Assign the user to the specific company using the UUID passed from the Controller
        newAdmin.setCompanyId(companyId);

        // 4. Encode password and save
        newAdmin.setPasswordHash(passwordEncoder.encode(newAdmin.getPasswordHash()));

        userRepository.save(newAdmin);
    }

}