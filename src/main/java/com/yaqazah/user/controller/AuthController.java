package com.yaqazah.user.controller;

import com.yaqazah.user.dto.request.*;
import com.yaqazah.user.dto.response.LoginResponseDto;
import com.yaqazah.user.service.AuthService;
import com.yaqazah.user.service.RefreshTokenService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@NullMarked
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;


    @PostMapping("/signup")
    public ResponseEntity<?> signup(
            @Valid @RequestBody UserRegistrationDto request
    ) {
        return ResponseEntity.ok(
                Map.of(
                        "message",
                        authService.signup(request)
                )
        );
    }

    @PostMapping("/register-owner")
    public ResponseEntity<?> registerOwner(
            @Valid @RequestBody CompanyOwnerRegistrationDto request
    ) {
        return ResponseEntity.ok(
                Map.of(
                        "message",
                        authService.registerCompanyOwner(request)
                )
        );
    }

    @PostMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(
            @Valid @RequestBody VerifyEmailDto request
    ) {

        LoginResponseDto response =
                authService.verifyEmail(request);


        if ("web".equalsIgnoreCase(request.getClient())) {

            ResponseCookie cookie =
                    ResponseCookie.from(
                                    "refresh_token",
                                    response.getRefreshToken()
                            )
                            .httpOnly(true)
                            .secure(true)
                            .path("/api/auth/refresh")
                            .maxAge(7 * 24 * 60 * 60)
                            .build();


            response.setRefreshToken(null);


            return ResponseEntity.ok()
                    .header(
                            HttpHeaders.SET_COOKIE,
                            cookie.toString()
                    )
                    .body(response);
        }


        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequestDto request
    ) {

        LoginResponseDto response =
                authService.login(request);


        if ("web".equalsIgnoreCase(request.getClient())) {


            ResponseCookie cookie =
                    ResponseCookie.from(
                                    "refresh_token",
                                    response.getRefreshToken()
                            )
                            .httpOnly(true)
                            .secure(true)
                            .path("/api/auth/refresh")
                            .maxAge(7 * 24 * 60 * 60)
                            .build();

            response.setRefreshToken(null);

            return ResponseEntity.ok()
                    .header(
                            HttpHeaders.SET_COOKIE,
                            cookie.toString()
                    )
                    .body(response);
        }


        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(
            @RequestBody(required = false)
            RefreshTokenDto body,

            @CookieValue(
                    name = "refresh_token",
                    required = false
            )
            String cookie
    ) {

        String token =
                body != null &&
                        body.getRefreshToken() != null
                        ?
                        body.getRefreshToken()
                        :
                        cookie;

        if (token == null) {
            throw new IllegalArgumentException(
                    "Refresh token missing"
            );
        }

        String accessToken =
                authService.refreshAccessToken(
                        token,
                        cookie != null ? "web" : "mobile"
                );

        return ResponseEntity.ok(
                Map.of(
                        "accessToken",
                        accessToken
                )
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            Authentication authentication
    ) {

        refreshTokenService.deleteByUserId(
                authentication.getName()
        );

        ResponseCookie cookie =
                ResponseCookie.from(
                                "refresh_token",
                                ""
                        )
                        .httpOnly(true)
                        .secure(true)
                        .path("/api/auth/refresh")
                        .maxAge(0)
                        .build();

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        cookie.toString()
                )
                .body(
                        Map.of(
                                "message",
                                "Logged out successfully"
                        )
                );
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(
            @Valid @RequestBody ForgotPasswordDto request
    ) {

        authService.requestPasswordReset(request);

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "Reset email sent"
                )
        );
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(
            @Valid @RequestBody ResetPasswordDto request
    ) {

        authService.resetPassword(request);


        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "Password reset successfully"
                )
        );
    }
}