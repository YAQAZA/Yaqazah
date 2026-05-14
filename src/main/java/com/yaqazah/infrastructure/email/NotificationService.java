package com.yaqazah.infrastructure.email;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final EmailService emailService;
    private final StringRedisTemplate redisTemplate;

    // SecureRandom is thread-safe and cryptographically strong
    private static final SecureRandom secureRandom = new SecureRandom();

    public static final String PREFIX_VERIFY = "OTP_VERIFY:";
    public static final String PREFIX_RESET = "OTP_RESET:";

    @Async
    public void sendVerificationEmail(String email) {
        generateAndSendOtp(email, PREFIX_VERIFY, "Verify your Yaqazah Account", 15);
    }

    @Async
    public void sendPasswordResetOtp(String email) {
        generateAndSendOtp(email, PREFIX_RESET, "Password Reset Request", 10);
    }

    private void generateAndSendOtp(String email, String prefix, String subject, int minutes) {
        try {
            // Generate a secure 6-digit OTP
            String otp = String.format("%06d", secureRandom.nextInt(1000000));

            // Store in Redis
            redisTemplate.opsForValue().set(prefix + email, otp, minutes, TimeUnit.MINUTES);

            // Use the EmailService tool to send it
            String messageBody = "Your code is: " + otp + "\nThis code will expire in " + minutes + " minutes.";
            emailService.sendEmail(email, subject, messageBody);

            log.info("Successfully sent {} to {}", subject, email);
        } catch (Exception e) {
            // Crucial: Async errors must be caught and logged
            log.error("Failed to send notification to {}: {}", email, e.getMessage());
        }
    }
}