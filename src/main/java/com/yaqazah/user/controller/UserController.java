package com.yaqazah.user.controller;

import com.yaqazah.user.model.User;
import com.yaqazah.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/me")
@PreAuthorize("isAuthenticated()") // Anyone logged in can access this
public class UserController {

    @Autowired
    private UserService userService;

    // Get current user's email securely from the token
    private String getCurrentUserEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

//    @PutMapping("/edit")
//    public ResponseEntity<String> updateMyProfile(@RequestBody User updatedData) {
//        userService.updateMyProfile(getCurrentUserEmail(), updatedData);
//        return ResponseEntity.ok("Your profile has been updated.");
//    }
//
//    @DeleteMapping("/delete")
//    public ResponseEntity<String> deleteMyProfile() {
//        userService.deleteMyProfile(getCurrentUserEmail());
//        return ResponseEntity.ok("Your account has been deleted.");
//    }
}