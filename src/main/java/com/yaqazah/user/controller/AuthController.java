package com.yaqazah.user.controller;

import com.yaqazah.common.security.JwtUtil;
import com.yaqazah.user.dto.CompanyOwnerRegistrationDto;
import com.yaqazah.user.dto.LoginResponseDto;
import com.yaqazah.user.dto.UserRegistrationDto;
import com.yaqazah.user.service.AuthService;
import com.yaqazah.user.service.RefreshTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException; // <-- Added Import
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@NullMarked
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Endpoints for user registration, login, and password management")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;

    @PostMapping("/signup")
    @Operation(summary = "Sign up a new user")
    public ResponseEntity<String> signup(@Valid @RequestBody UserRegistrationDto request) {
        try {
            String message = authService.signup(request);
            return ResponseEntity.ok(message);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/register-owner")
    @Operation(summary = "Register a new Company Owner and create their Company workspace")
    public ResponseEntity<String> registerCompanyOwner(@RequestBody CompanyOwnerRegistrationDto request) {
        try {
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
            String client = payload.getOrDefault("client", "web");

            LoginResponseDto response = authService.verifyEmail(
                    payload.get("email"),
                    payload.get("otp"),
                    client
            );

            // Web vs Mobile Split for Refresh Tokens
            if ("web".equalsIgnoreCase(client)) {
                ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", response.getRefreshToken())
                        .httpOnly(true)
                        .secure(true) // Set to false if testing on localhost without HTTPS
                        .path("/api/auth/refresh")
                        .maxAge(7 * 24 * 60 * 60)
                        .build();

                // Remove token from JSON so JS cannot steal it
                response.setRefreshToken(null);

                return ResponseEntity.ok()
                        .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                        .body(Map.of(
                                "message", "Email verified successfully!",
                                "data", response
                        ));
            }

            return ResponseEntity.ok(Map.of(
                    "message", "Email verified successfully!",
                    "data", response
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/login")
    @Operation(summary = "User login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> payload) {
        try {
            String email = payload.get("email");
            String password = payload.get("password");
            String client = payload.getOrDefault("client", "web");

            LoginResponseDto response = authService.login(email, password, client);

            // Web vs Mobile Split for Refresh Tokens
            if ("web".equalsIgnoreCase(client)) {
                ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", response.getRefreshToken())
                        .httpOnly(true)
                        .secure(true) // Set to false if testing on localhost without HTTPS
                        .path("/api/auth/refresh")
                        .maxAge(7 * 24 * 60 * 60)
                        .build();

                // Remove token from JSON so JS cannot steal it
                response.setRefreshToken(null);

                return ResponseEntity.ok()
                        .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                        .body(response);
            }

            // Mobile gets both tokens in the JSON response
            return ResponseEntity.ok(response);

            // --- THE FIX: Better exception handling hierarchy ---
        } catch (SecurityException e) {
            // Device vs Role policy violations
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (AuthenticationException e) {
            // Spring Security authentication failures (bad password/email)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid credentials"));
        } catch (Exception e) {
            // Database crashes, null pointers, etc.
            e.printStackTrace(); // Keep this so you can see it in your console
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Server error occurred during login."));
        }
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh expired access token")
    public ResponseEntity<?> refreshToken(
            @RequestBody(required = false) Map<String, String> payload,
            @CookieValue(name = "refresh_token", required = false) String cookieToken
    ) {
        try {
            // 1. Find the token in either the request body or the cookie
            String requestRefreshToken = (payload != null && payload.containsKey("refreshToken"))
                    ? payload.get("refreshToken")
                    : cookieToken;

            if (requestRefreshToken == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Refresh Token is missing!"));
            }

            // 2. Determine the client type based on how the token arrived
            String client = (cookieToken != null) ? "web" : "mobile";

            // 3. Let the Service handle the business logic!
            String newAccessToken = authService.refreshAccessToken(requestRefreshToken, client);

            return ResponseEntity.ok(Map.of(
                    "accessToken", newAccessToken,
                    "message", "Token refreshed successfully"
            ));

        } catch (SecurityException e) {
            // Catches the error if the refresh token is expired or not in the database
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "Log the user out and destroy the refresh token")
    public ResponseEntity<?> logout(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");

        // 1. Delete from DB (Revokes access for both mobile and web)
        if (email != null) {
            refreshTokenService.deleteByUserId(email);
        }

        // 2. Erase the Web Cookie by setting maxAge to 0
        ResponseCookie clearCookie = ResponseCookie.from("refresh_token", "")
                .httpOnly(true)
                .secure(true)
                .path("/api/auth/refresh")
                .maxAge(0)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, clearCookie.toString())
                .body(Map.of("message", "Logged out successfully"));
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