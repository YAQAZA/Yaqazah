package com.yaqazah.user.controller;

import com.yaqazah.user.model.User;
import com.yaqazah.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/company/drivers")
@PreAuthorize("hasRole('COMPANY_ADMIN')")
public class CompanyDriverController {

    @Autowired
    private UserService userService;

    @PostMapping("/add")
    public ResponseEntity<String> addFleetDriver(@RequestBody User newDriver) {

        // 1. Get the current user's email from the Security Context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentAdminEmail = authentication.getName();

        try {
            // 2. Pass the data to the Service layer to do all the hard work
            userService.addFleetDriver(newDriver, currentAdminEmail);

            return ResponseEntity.ok("Fleet Driver added successfully to your company!");

        } catch (IllegalArgumentException e) {
            // Catch the "Email already taken" error
            return ResponseEntity.badRequest().body(e.getMessage());

        } catch (IllegalStateException e) {
            // Catch the "Admin not found" error
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }
}