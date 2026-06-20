package com.yaqazah.user.controller;

import com.yaqazah.user.dto.CompanyAdminDto;
import com.yaqazah.user.dto.CompanyOwnerRegistrationDto;
import com.yaqazah.user.service.AdminService;
import com.yaqazah.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@NullMarked
@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin User Management", description = "Endpoints for Super Admins to manage high-level users and companies")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final UserService userProfileService;

    @Operation(summary = "Register a new Company Owner and create their Company")
    @PostMapping("/register-owner")
    public ResponseEntity<?> registerCompanyOwner(@RequestBody CompanyOwnerRegistrationDto request) {
        try {
            adminService.registerCompanyOwner(request);
            return ResponseEntity.ok("Company owner and company registered successfully.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/add-company-admin")
    @Operation(summary = "Add a Company Admin", description = "Creates a new Company Admin and assigns them to the logged-in admin's company.")
    public ResponseEntity<String> addCompanyAdminUser(
            @Valid @RequestBody CompanyAdminDto req,
            Principal principal) { // Assuming you use Principal, or your getCurrentAdminEmail() method

        try {
            // If you have a base controller method getCurrentAdminEmail(), use that instead of principal.getName()
            adminService.addCompanyAdmin(req, principal.getName());
            return ResponseEntity.ok("Company Admin created successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
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

//    @Operation(summary = "Add a Company Admin", description = "Creates a new Company Admin and assigns them to the logged-in user's company.")
//    @PostMapping("/add-company-admin")
//    public ResponseEntity<String> addCompanyAdminUser(
//            Principal principal,
//            @Valid @RequestBody CompanyAdminDto req) {
//
//        try {
//            // principal.getName() securely gets the logged-in user's email
//            adminService.addCompanyAdmin(principal.getName(), req);
//            return ResponseEntity.ok("Company Admin created and assigned to your company successfully");
//        } catch (IllegalArgumentException e) {
//            return ResponseEntity.badRequest().body(e.getMessage());
//        }
//    }

}