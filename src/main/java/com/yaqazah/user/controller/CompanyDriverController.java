package com.yaqazah.user.controller;

import com.yaqazah.user.model.User;
import com.yaqazah.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/company/drivers")
@PreAuthorize("hasRole('COMPANY_ADMIN')")
public class CompanyDriverController {

    @Autowired
    private UserService userService;

    @PostMapping("/add")
    public ResponseEntity<String> addFleetDriver(@RequestBody User newDriver) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentAdminEmail = authentication.getName();

        try {
            userService.addFleetDriver(newDriver, currentAdminEmail);
            return ResponseEntity.ok("Fleet Driver added successfully to your company!");

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());

        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    // --- NEW EDIT ENDPOINT ---

    @PutMapping("/edit/{driverId}")
    public ResponseEntity<String> editFleetDriver(
            @PathVariable UUID driverId,
            @RequestBody User updatedDriverData) {

        // 1. Get the current user's email from the Security Context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentAdminEmail = authentication.getName();

        try {
            // 2. Pass data to service
            userService.updateFleetDriver(driverId, updatedDriverData, currentAdminEmail);
            return ResponseEntity.ok("Fleet Driver updated successfully!");

        } catch (IllegalArgumentException e) {
            // Catch "Driver not found" or wrong role
            return ResponseEntity.badRequest().body(e.getMessage());

        } catch (IllegalStateException e) {
            // Catch cross-company editing attempts (403 Forbidden is best here)
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }
}