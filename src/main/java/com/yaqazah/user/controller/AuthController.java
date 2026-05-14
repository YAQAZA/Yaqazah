package com.yaqazah.user.controller;

import com.yaqazah.user.model.User;
import com.yaqazah.user.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Endpoints for user registration, login, and password management")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // 1. SIGN UP
    @PostMapping("/signup")
    @Operation(summary = "Sign up a new user", description = "Registers a new user and sends an OTP for email verification.")
    public ResponseEntity<?> signup(@RequestBody User user) {
        try {
            String message = authService.signup(user);
            return ResponseEntity.ok(message);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 2. VERIFY EMAIL
    @PostMapping("/verify-email")
    @Operation(summary = "Verify email with OTP", description = "Verifies the user's email using the OTP sent during signup.")
    public ResponseEntity<?> verifyEmail(@RequestBody Map<String, String> payload) {
        try {
            String email = payload.get("email");
            String otp = payload.get("otp");

            String jwt = authService.verifyEmail(email, otp);

            return ResponseEntity.ok(Map.of(
                    "message", "Email verified successfully!",
                    "token", jwt
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 3. LOG IN
    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticates a user and returns a JWT token.")
    public ResponseEntity<?> login(@RequestBody Map<String, String> payload) {
        try {
            String email = payload.get("email");
            String password = payload.get("password");

            String jwt = authService.login(email, password);
            return ResponseEntity.ok(jwt);
        } catch (Exception e) {
            // Catches BadCredentialsException from Spring Security
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
        }
    }

    // 4. FORGOT PASSWORD
    @PostMapping("/forgot-password")
    @Operation(summary = "Forgot password", description = "Triggers an OTP to be sent to the user's email for password reset.")
    public ResponseEntity<String> forgotPassword(@RequestBody Map<String, String> payload) {
        try {
            authService.sendPasswordResetOtp(payload.get("email"));
            return ResponseEntity.ok("Reset code sent to your email.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 5. RESET PASSWORD
    @PostMapping("/reset-password")
    @Operation(summary = "Reset password", description = "Resets the user's password using the provided OTP and new password.")
    public ResponseEntity<String> resetPassword(@RequestBody Map<String, String> payload) {
        try {
            String email = payload.get("email");
            String otp = payload.get("otp");
            String newPassword = payload.get("newPassword");

            authService.resetPassword(email, otp, newPassword);
            return ResponseEntity.ok("Password has been reset successfully!");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}