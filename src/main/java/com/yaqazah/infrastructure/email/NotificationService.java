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
    private static final SecureRandom secureRandom = new SecureRandom();

    public static final String PREFIX_VERIFY = "OTP_VERIFY:";
    public static final String PREFIX_RESET = "OTP_RESET:";
    private static final String PREFIX_LIMIT = "LIMIT:"; // For anti-spam

    @Async
    public void sendVerificationEmail(String email) {
        checkRateLimitAndSend(email, PREFIX_VERIFY, "Verify your Yaqazah Account", 15);
    }

    @Async
    public void sendPasswordResetOtp(String email) {
        checkRateLimitAndSend(email, PREFIX_RESET, "Password Reset Request", 10);
    }

    private void checkRateLimitAndSend(String email, String prefix, String subject, int minutes) {
        String limitKey = PREFIX_LIMIT + prefix + email;

        // Check if a request was made in the last 60 seconds
        Boolean isLimited = redisTemplate.hasKey(limitKey);
        if (Boolean.TRUE.equals(isLimited)) {
            log.warn("Spam detected: {} is requesting OTP too fast.", email);
            // Since this is @Async, we log it. The Controller should ideally
            // check the rate limit before calling this if you want to show a 429 error.
            return;
        }

        try {
            String otp = String.format("%06d", secureRandom.nextInt(1000000));

            // Store the OTP
            redisTemplate.opsForValue().set(prefix + email, otp, minutes, TimeUnit.MINUTES);

            // Set the Rate Limit key for 60 seconds
            redisTemplate.opsForValue().set(limitKey, "LOCKED", 1, TimeUnit.MINUTES);

            emailService.sendEmail(email, subject, "Your code is: " + otp);
            log.info("OTP sent successfully to {}", email);
        } catch (Exception e) {
            log.error("Failed to send OTP to {}: {}", email, e.getMessage());
        }
    }

    @Async
    public void sendWelcomePasswordSetEmail(String email) {
        try {
            // 1. Generate an OTP (acting as a secure token)
            String otp = String.format("%06d", secureRandom.nextInt(1000000));

            // 2. Store it in Redis using the RESET prefix.
            // We give them 24 hours (instead of 10 mins) to set their initial password.
            redisTemplate.opsForValue().set(PREFIX_RESET + email, otp, 3, TimeUnit.DAYS);
            // (Replace "localhost:3000" with your actual frontend URL)
            String frontendResetUrl = "http://localhost:3000/set-password?email=" + email + "&otp=" + otp;

            // 4. Send the email
            String subject = "Welcome to Yaqazah! Please set your password";
            String body = "Hello,\n\n" +
                    "Your account has been created by your administrator.\n" +
                    "Please click the link below to set your secure password and activate your account:\n\n" +
                    frontendResetUrl;

            emailService.sendEmail(email, subject, body);
            log.info("Welcome/Set Password link sent successfully to {}", email);

        } catch (Exception e) {
            log.error("Failed to send Welcome link to {}: {}", email, e.getMessage());
        }
    }
}