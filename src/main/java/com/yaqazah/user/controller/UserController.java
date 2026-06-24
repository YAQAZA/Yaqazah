package com.yaqazah.user.controller;

import com.yaqazah.user.dto.LoginRequestDto;
import com.yaqazah.user.dto.UserProfileResponseDto;
import com.yaqazah.user.model.User;
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
        try {
            String newName = payload.get("fullName");
            String email = getCurrentUserEmail();

            userService.updateUserName(email, newName);
            return ResponseEntity.ok("Name updated successfully!");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping
    @Operation(summary = "Delete My Account", description = "Permanently deletes the currently authenticated user's account.")
    public ResponseEntity<String> deleteMyAccount() {
        try {
            User currentUser = userService.findByEmail(getCurrentUserEmail());
            userService.deleteAccount(currentUser.getUserId());
            return ResponseEntity.ok("Your account has been deleted successfully.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/restore")
    @Operation(summary = "Restore Account", description = "Public endpoint to restore a deleted account using email and password.")
    public ResponseEntity<String> restoreAccount(@RequestBody LoginRequestDto request) {
        try {
            userService.restoreAccount(request.getEmail(), request.getPassword());
            return ResponseEntity.ok("Account successfully restored.");
        } catch (IllegalArgumentException | IllegalStateException | SecurityException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}