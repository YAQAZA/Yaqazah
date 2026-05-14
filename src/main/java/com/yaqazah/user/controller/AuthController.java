package com.yaqazah.user.controller;

import com.yaqazah.common.security.JwtUtil;
import com.yaqazah.user.model.Role;
import com.yaqazah.user.model.User;
import com.yaqazah.user.repository.UserRepository;
import com.yaqazah.user.service.AuthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private UserDetailsService userDetailsService;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private AuthService authService;

    // 1. SIGN UP
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody User user) {
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body("Email is already taken!");
        }

        // Security Overrides
        user.setRole(Role.INDEPENDENT_DRIVER);
        user.setCompany(null);
        user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));

        userRepository.save(user);

        // Send the 6-digit OTP immediately
        authService.sendVerificationEmail(user.getEmail());

        return ResponseEntity.ok("User registered! Check your email for the verification code.");
    }

    // 2. VERIFY EMAIL
    @PostMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestBody Map<String, String> payload) {
        try {
            String email = payload.get("email");
            String otp = payload.get("otp");

            // Logic inside AuthService checks if OTP is valid for this email
            authService.verifyEmail(email, otp);

            // Generate the JWT immediately upon successful verification
            final UserDetails userDetails = userDetailsService.loadUserByUsername(email);
            final String jwt = jwtUtil.generateToken(userDetails);

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
    public ResponseEntity<String> login(@RequestBody Map<String, String> payload) {

        String email = payload.get("email");
        String password = payload.get("password");

        // Standard authentication
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));

        final UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        final String jwt = jwtUtil.generateToken(userDetails);

        return ResponseEntity.ok(jwt);
    }

    // 4. FORGOT PASSWORD (Trigger OTP)
    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestBody Map<String, String> payload) {
        try {
            String email = payload.get("email");
            authService.sendPasswordResetOtp(email);
            return ResponseEntity.ok("Reset code sent to your email.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 5. RESET PASSWORD (Submit OTP + New Password)
    @PostMapping("/reset-password")
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