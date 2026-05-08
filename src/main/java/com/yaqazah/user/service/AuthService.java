package com.yaqazah.user.service;

import com.yaqazah.user.model.*;
import com.yaqazah.user.repository.*;
import com.yaqazah.infrastructure.email.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Random;

@Service
public class AuthService {
    @Autowired private UserRepository userRepository;
    @Autowired private AuthTokenRepository tokenRepository;
    @Autowired private EmailService emailService;
    @Autowired private PasswordEncoder passwordEncoder;

    public void sendVerificationEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String otp = String.format("%06d", new Random().nextInt(999999));
        AuthToken token = new AuthToken();
        token.setToken(otp);
        token.setTokenType(TokenType.EMAIL_VERIFICATION);
        token.setExpiryDate(LocalDateTime.now().plusMinutes(15));
        token.setUser(user);
        tokenRepository.save(token);

        emailService.sendEmail(user.getEmail(), "Verify your Yaqazah Account", "Code: " + otp);
    }

    @Transactional
    public void verifyEmail(String email, String otp) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("User not found"));
        AuthToken tokenRecord = tokenRepository.findByTokenAndUserAndTokenType(otp, user, TokenType.EMAIL_VERIFICATION)
                .orElseThrow(() -> new IllegalArgumentException("Invalid code"));

        if (tokenRecord.isExpired()) throw new IllegalArgumentException("Code expired.");

        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
        tokenRepository.delete(tokenRecord);
    }
    @Transactional
    public void sendPasswordResetOtp(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // 1. Clean up any old reset tokens for this user so they don't clutter the DB
        // Note: You may need to add deleteByUserAndTokenType to your AuthTokenRepository
        tokenRepository.deleteByUserAndTokenType(user, TokenType.PASSWORD_RESET);

        // 2. Generate 6-digit OTP
        String otp = String.format("%06d", new java.util.Random().nextInt(999999));

        // 3. Save the token
        AuthToken token = new AuthToken();
        token.setToken(otp);
        token.setTokenType(TokenType.PASSWORD_RESET);
        token.setExpiryDate(LocalDateTime.now().plusMinutes(10)); // Shorter window for security
        token.setUser(user);
        tokenRepository.save(token);

        // 4. Send Email
        emailService.sendEmail(user.getEmail(), "Password Reset Request", "Your reset code is: " + otp);
    }

    @Transactional
    public void resetPassword(String email, String otp, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Verify the OTP specifically for PASSWORD_RESET type
        AuthToken tokenRecord = tokenRepository.findByTokenAndUserAndTokenType(otp, user, TokenType.PASSWORD_RESET)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired reset code."));

        if (tokenRecord.isExpired()) {
            throw new IllegalArgumentException("Reset code has expired.");
        }

        // Update password and clean up
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        tokenRepository.delete(tokenRecord);
    }
}