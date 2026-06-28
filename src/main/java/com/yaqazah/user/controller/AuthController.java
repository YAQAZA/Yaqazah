package com.yaqazah.user.controller;

import com.yaqazah.user.dto.request.*;
import com.yaqazah.user.dto.response.LoginResponseDto;
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
import org.springframework.security.core.AuthenticationException;
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
    public ResponseEntity<?> signup(@Valid @RequestBody UserRegistrationDto request) {
        try {
            String message = authService.signup(request);
            return ResponseEntity.ok(Map.of("message", message));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/register-owner")
    @Operation(summary = "Register a new Company Owner and create their Company workspace")
    public ResponseEntity<?> registerCompanyOwner(@Valid @RequestBody CompanyOwnerRegistrationDto request) {
        try {
            String message = authService.registerCompanyOwner(request);
            return ResponseEntity.ok(Map.of("message", message));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/verify-email")
    @Operation(summary = "Verify email with OTP")
    public ResponseEntity<?> verifyEmail(@Valid @RequestBody VerifyEmailDto request) {
        try {
            LoginResponseDto response = authService.verifyEmail(request);

            if ("web".equalsIgnoreCase(request.getClient())) {
                ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", response.getRefreshToken())
                        .httpOnly(true)
                        .secure(true) // Set to false if testing on localhost without HTTPS
                        .path("/api/auth/refresh")
                        .maxAge(7 * 24 * 60 * 60)
                        .build();

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
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDto request) {
        try {
            LoginResponseDto response = authService.login(request);

            if ("web".equalsIgnoreCase(request.getClient())) {
                ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", response.getRefreshToken())
                        .httpOnly(true)
                        .secure(true) // Set to false if testing on localhost without HTTPS
                        .path("/api/auth/refresh")
                        .maxAge(7 * 24 * 60 * 60)
                        .build();

                response.setRefreshToken(null);

                return ResponseEntity.ok()
                        .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                        .body(response);
            }

            return ResponseEntity.ok(response);

        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid credentials"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Server error occurred during login."));
        }
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh expired access token")
    public ResponseEntity<?> refreshToken(
            @RequestBody(required = false) RefreshTokenDto payload,
            @CookieValue(name = "refresh_token", required = false) String cookieToken
    ) {
        try {
            String requestRefreshToken = (payload != null && payload.getRefreshToken() != null)
                    ? payload.getRefreshToken()
                    : cookieToken;

            if (requestRefreshToken == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Refresh Token is missing!"));
            }

            String client = (cookieToken != null) ? "web" : "mobile";

            String newAccessToken = authService.refreshAccessToken(requestRefreshToken, client);

            return ResponseEntity.ok(Map.of(
                    "accessToken", newAccessToken,
                    "message", "Token refreshed successfully"
            ));

        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "Log the user out and destroy the refresh token")
    public ResponseEntity<?> logout(@Valid @RequestBody LogoutRequestDto request) {

        refreshTokenService.deleteByUserId(request.getEmail());

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
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordDto request) {
        try {
            authService.requestPasswordReset(request);
            return ResponseEntity.ok(Map.of("message", "If the email exists, a reset code has been sent."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordDto request) {
        try {
            authService.resetPassword(request);
            return ResponseEntity.ok(Map.of("message", "Password has been reset successfully!"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}