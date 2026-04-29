package com.yaqazah.user.controller;

import com.yaqazah.user.model.User;
import com.yaqazah.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    // Inject the Service instead of the Repository/Encoder
    @Autowired
    private UserService userService;

    @PostMapping("/companies/{companyId}/add-company-admin")
    public ResponseEntity<String> addCompanyAdminUser(
            @PathVariable UUID companyId,
            @RequestBody User user) {

        try {
            // Hand the data off to the Service layer
            userService.addCompanyAdmin(companyId, user);

            return ResponseEntity.ok("Company Admin added successfully to company ID: " + companyId);

        } catch (IllegalArgumentException e) {
            // Catch the "Email already taken" exception from the service
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}