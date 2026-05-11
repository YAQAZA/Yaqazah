package com.yaqazah.user.service;

import com.yaqazah.user.model.*;
import com.yaqazah.user.repository.*;
import com.yaqazah.infrastructure.email.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
public class AuthService {

    @Autowired private UserRepository userRepository;
    @Autowired private EmailService emailService;
    @Autowired private PasswordEncoder passwordEncoder;

    // 1. Inject Redis instead of AuthTokenRepository!
    @Autowired private StringRedisTemplate redisTemplate;

    // Prefixes to keep your Redis database organized
    private static final String PREFIX_VERIFY = "OTP_VERIFY:";
    private static final String PREFIX_RESET = "OTP_RESET:";

    @Async
    public void sendVerificationEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String otp = String.format("%06d", new Random().nextInt(999999));

        // 2. Save to Redis with a 15-minute Time-To-Live (TTL)
        redisTemplate.opsForValue().set(PREFIX_VERIFY + email, otp, 15, TimeUnit.MINUTES);

        emailService.sendEmail(user.getEmail(), "Verify your Yaqazah Account", "Code: " + otp);
    }

    @Transactional
    public void verifyEmail(String email, String otp) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String redisKey = PREFIX_VERIFY + email;

        // 3. Fetch from Redis
        String storedOtp = redisTemplate.opsForValue().get(redisKey);

        if (storedOtp == null) {
            throw new IllegalArgumentException("Code has expired or is invalid.");
        }
        if (!storedOtp.equals(otp)) {
            throw new IllegalArgumentException("Invalid code.");
        }

        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        // 4. Delete from Redis so it can't be reused
        redisTemplate.delete(redisKey);
    }

    @Async // Don't forget to make this async too!
    public void sendPasswordResetOtp(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String otp = String.format("%06d", new Random().nextInt(999999));

        // Redis automatically overwrites the old OTP if the user requests a new one!
        // No manual cleanup needed.
        redisTemplate.opsForValue().set(PREFIX_RESET + email, otp, 10, TimeUnit.MINUTES);

        emailService.sendEmail(user.getEmail(), "Password Reset Request", "Your reset code is: " + otp);
    }

    @Transactional
    public void resetPassword(String email, String otp, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String redisKey = PREFIX_RESET + email;
        String storedOtp = redisTemplate.opsForValue().get(redisKey);

        if (storedOtp == null) {
            throw new IllegalArgumentException("Reset code has expired.");
        }
        if (!storedOtp.equals(otp)) {
            throw new IllegalArgumentException("Invalid reset code.");
        }

        // Update password and clean up Redis
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        redisTemplate.delete(redisKey);
    }
}