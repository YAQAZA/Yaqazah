package com.yaqazah.user.controller;

import com.yaqazah.user.dto.response.AdminCompanyDashboardDto;
import com.yaqazah.user.dto.response.CompanyInfoDto;
import com.yaqazah.user.dto.request.LoginRequestDto;
import com.yaqazah.user.dto.response.UserProfileResponseDto;
import com.yaqazah.user.model.User;
import com.yaqazah.user.dto.response.AdminListDto;
import com.yaqazah.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@NullMarked
@RestController
@RequestMapping("/api/users/me")
@PreAuthorize("isAuthenticated()")
@Tag(name = "User Profile", description = "Endpoints for logged-in users to view and manage their own profile")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    private String getCurrentUserEmail() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("User is not authenticated");
        }

        return authentication.getName();
    }

    @GetMapping
    @Operation(summary = "Get My Profile", description = "Fetches the profile details of the currently authenticated user.")
    public ResponseEntity<UserProfileResponseDto> getMyProfile() {

        // 1. Get the current user's email from the security context
        String email = getCurrentUserEmail();

        // 2. Ask the service layer for the pre-mapped DTO
        UserProfileResponseDto response = userService.getUserProfileDto(email);

        // 3. Return the response
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/update-name")
    @Operation(summary = "Update Full Name", description = "Updates the full name of the currently authenticated user.")
    public ResponseEntity<String> updateMyName(@RequestBody Map<String, String> payload) {
        String newName = payload.get("fullName");
        String email = getCurrentUserEmail();

        userService.updateUserName(email, newName);
        return ResponseEntity.ok("Name updated successfully!");
    }

    @DeleteMapping
    @Operation(summary = "Delete My Account", description = "Permanently deletes the currently authenticated user's account.")
    public ResponseEntity<String> deleteMyAccount() {
        User currentUser = userService.findByEmail(getCurrentUserEmail());
        userService.deleteAccount(currentUser.getUserId());
        return ResponseEntity.ok("Your account has been deleted successfully.");
    }

    @PostMapping("/restore")
    @Operation(summary = "Restore Account", description = "Public endpoint to restore a deleted account using email and password.")
    public ResponseEntity<String> restoreAccount(@RequestBody LoginRequestDto request) {
        userService.restoreAccount(request.getEmail(), request.getPassword());
        return ResponseEntity.ok("Account successfully restored.");
    }
//    @GetMapping("/company-admins")
//    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_ADMIN')")
//    @Operation(summary = "Get All Company Admins", description = "Fetches a list of all admins and company admins belonging to the requester's company.")
//    public ResponseEntity<List<AdminListDto>> getCompanyAdmins() {
//        String email = getCurrentUserEmail();
//        List<AdminListDto> response = userService.getCompanyAdmins(email);
//
//        return ResponseEntity.ok(response);
//    }

//    @GetMapping("/company-info")
//    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_ADMIN', 'FLEET_DRIVER')") // Note: Use hasAnyRole if your DB uses the "ROLE_" prefix
//    @Operation(summary = "Get Company Overview", description = "Fetches the company details along with the primary owner's info and total admin count.")
//    public ResponseEntity<CompanyInfoDto> getCompanyInfo() {
//        // Securely extract the email from the logged-in user's token
//        String email = SecurityContextHolder.getContext().getAuthentication().getName();
//
//        // Fetch the mapped DTO from the service
//        CompanyInfoDto response = userService.getCompanyInfo(email);
//
//        return ResponseEntity.ok(response);
//    }

    @GetMapping("/admin-view")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_ADMIN')")
    @Operation(
            summary = "Get Admin Company Dashboard",
            description = "Returns user profile, company info, and company admins"
    )
    public ResponseEntity<AdminCompanyDashboardDto> getAdminDashboard() {

        String email = getCurrentUserEmail();

        AdminCompanyDashboardDto response =
                userService.getAdminCompanyDashboard(email);

        return ResponseEntity.ok(response);
    }

}