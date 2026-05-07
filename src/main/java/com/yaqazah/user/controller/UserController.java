package com.yaqazah.user.controller;

import com.yaqazah.user.model.User;
import com.yaqazah.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users/me")
@PreAuthorize("isAuthenticated()") // Any logged-in user can access this
public class UserController {

    @Autowired
    private UserService userService;

    private String getCurrentUserEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }


    @GetMapping
    public ResponseEntity<User> getMyProfile() {
        String email = getCurrentUserEmail();
        User user = userService.findByEmail(email);
        return ResponseEntity.ok(user);
    }


    @PatchMapping("/update-name")
    public ResponseEntity<String> updateMyName(@RequestBody Map<String, String> payload) {
        String newName = payload.get("fullName");

        if (newName == null || newName.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Full name cannot be empty.");
        }

        String email = getCurrentUserEmail();
        userService.updateUserName(email, newName);

        return ResponseEntity.ok("Name updated successfully!");
    }
}