package com.yaqazah.user.controller;

import com.yaqazah.user.dto.CompanyAdminDto;
import com.yaqazah.user.dto.CompanyOwnerRegistrationDto;
import com.yaqazah.user.dto.FleetDriverDto;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

    private String getCurrentAdminEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }

    @PostMapping("/register-owner")
    @Operation(summary = "Register a new Company Owner and create their Company")
    public ResponseEntity<?> registerCompanyOwner(@Valid @RequestBody CompanyOwnerRegistrationDto request) {
        try {
            adminService.registerCompanyOwner(request);
            return ResponseEntity.ok("Company owner and company registered successfully.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/add-company-admin")
    @Operation(summary = "Add a Company Admin", description = "Creates a new Company Admin and assigns them to the logged-in admin's company.")
    public ResponseEntity<String> addCompanyAdminUser(@Valid @RequestBody CompanyAdminDto req, Principal principal) {
        try {
            // Securely gets the logged-in admin's email via Principal
            adminService.addCompanyAdmin(req, principal.getName());
            return ResponseEntity.ok("Company Admin created successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    @PostMapping("/add")
    @Operation(summary = "Add a new Fleet Driver", description = "Creates a new driver account linked to the logged-in admin's company.")
    public ResponseEntity<String> addFleetDriver(@Valid @RequestBody FleetDriverDto newDriver) {
        try {
            adminService.addFleetDriver(newDriver, getCurrentAdminEmail());
            return ResponseEntity.ok("Fleet Driver added successfully to your company!");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
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

    @PutMapping("/restore")
    @Operation(summary = "Restore Deleted Account", description = "Restores a soft-deleted user (and their company/drivers if applicable) before the 30-day hard delete.")
    public ResponseEntity<String> restoreUserAccount(@PathVariable UUID userId) {
        try {
            adminService.restoreAccount(userId);
            return ResponseEntity.ok("Account successfully restored.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}