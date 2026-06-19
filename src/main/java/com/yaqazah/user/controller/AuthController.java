package com.yaqazah.user.controller;

import com.yaqazah.user.dto.CompanyOwnerRegistrationDto;
import com.yaqazah.user.model.User;
import com.yaqazah.user.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Endpoints for user registration, login, and password management")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    @Operation(summary = "Sign up a new user")
    public ResponseEntity<String> signup(@RequestBody User user) {
        try {
            String message = authService.signup(user);
            return ResponseEntity.ok(message);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/register-owner")
    @Operation(summary = "Register a new Company Owner and create their Company workspace")
    public ResponseEntity<String> registerCompanyOwner(@RequestBody CompanyOwnerRegistrationDto request) {
        try {
            // Now captures the dynamic message (fresh signup vs. resend OTP) from the service
            String message = authService.registerCompanyOwner(request);
            return ResponseEntity.ok(message);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/verify-email")
    @Operation(summary = "Verify email with OTP")
    public ResponseEntity<?> verifyEmail(@RequestBody Map<String, String> payload) {
        try {
            String jwt = authService.verifyEmail(payload.get("email"), payload.get("otp"));
            return ResponseEntity.ok(Map.of(
                    "message", "Email verified successfully!",
                    "token", jwt
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/login")
    @Operation(summary = "User login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> payload) {
        try {
            String jwt = authService.login(payload.get("email"), payload.get("password"));
            return ResponseEntity.ok(Map.of("token", jwt));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
        }
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Forgot password")
    public ResponseEntity<String> forgotPassword(@RequestBody Map<String, String> payload) {
        try {
            authService.requestPasswordReset(payload.get("email"));
            return ResponseEntity.ok("If the email exists, a reset code has been sent.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password")
    public ResponseEntity<String> resetPassword(@RequestBody Map<String, String> payload) {
        try {
            authService.resetPassword(
                    payload.get("email"),
                    payload.get("otp"),
                    payload.get("newPassword")
            );
            return ResponseEntity.ok("Password has been reset successfully!");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}