package com.yaqazah.user.controller;

import com.yaqazah.user.model.User;
import com.yaqazah.user.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users/me")
@PreAuthorize("isAuthenticated()")
@Tag(name = "User Profile", description = "Endpoints for logged-in users to view and manage their own profile")
@RequiredArgsConstructor
public class UserController {

    private final UserProfileService userProfileService;

    private String getCurrentUserEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    @GetMapping
    @Operation(summary = "Get My Profile", description = "Fetches the profile details of the currently authenticated user.")
    public ResponseEntity<User> getMyProfile() {
        String email = getCurrentUserEmail();
        User user = userProfileService.findByEmail(email);
        return ResponseEntity.ok(user);
    }

    @PatchMapping("/update-name")
    @Operation(summary = "Update Full Name", description = "Updates the full name of the currently authenticated user.")
    public ResponseEntity<String> updateMyName(@RequestBody Map<String, String> payload) {
        try {
            String newName = payload.get("fullName");
            String email = getCurrentUserEmail();

            userProfileService.updateUserName(email, newName);
            return ResponseEntity.ok("Name updated successfully!");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping
    @Operation(summary = "Delete My Account", description = "Permanently deletes the currently authenticated user's account.")
    public ResponseEntity<String> deleteMyAccount() {
        try {
            User currentUser = userProfileService.findByEmail(getCurrentUserEmail());
            userProfileService.deleteAccount(currentUser.getUserId());
            return ResponseEntity.ok("Your account has been deleted successfully.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}