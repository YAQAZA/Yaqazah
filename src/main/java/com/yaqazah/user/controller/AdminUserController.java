package com.yaqazah.user.controller;

import com.yaqazah.user.dto.CompanyAdminDto;
import com.yaqazah.user.model.User;
import com.yaqazah.user.service.CompanyAdminService;
import com.yaqazah.user.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin User Management", description = "Endpoints for Super Admins to manage high-level users and companies")
@RequiredArgsConstructor
public class AdminUserController {

    private final CompanyAdminService companyAdminService;
    private final UserProfileService userProfileService;

//    @PostMapping("/companies/{companyId}/add-company-admin")
//    @Operation(summary = "Add a Company Admin", description = "Creates a new Company Admin and assigns them to the specified company.")
//    public ResponseEntity<String> addCompanyAdminUser(
//            @PathVariable UUID companyId,
//            @RequestBody User user) {
//        try {
//            companyAdminService.addCompanyAdmin(companyId, user);
//            return ResponseEntity.ok("Company Admin added successfully to company ID: " + companyId);
//        } catch (IllegalArgumentException e) {
//            return ResponseEntity.badRequest().body(e.getMessage());
//        }
//    }

    @PostMapping("/companies/{companyId}/add-company-admin")
    public ResponseEntity<String> addCompanyAdminUser(
            @PathVariable UUID companyId,
            @Valid @RequestBody CompanyAdminDto req) {

        companyAdminService.addCompanyAdmin(companyId, req);
        return ResponseEntity.ok("Company Admin created successfully");
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "Delete User by ID", description = "Allows a Super Admin to forcefully delete any user account.")
    public ResponseEntity<String> deleteUser(@PathVariable UUID userId) {
        try {
            userProfileService.deleteAccount(userId);
            return ResponseEntity.ok("User deleted successfully.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}