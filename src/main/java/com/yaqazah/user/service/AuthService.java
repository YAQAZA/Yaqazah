package com.yaqazah.user.service;

import com.yaqazah.common.security.JwtUtil;
import com.yaqazah.infrastructure.email.NotificationService;
import com.yaqazah.user.model.Role;
import com.yaqazah.user.model.User;
import com.yaqazah.user.model.UserStatus;
import com.yaqazah.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;

    @Transactional
    public String signup(User user) {
        Optional<User> existingUserOpt = userRepository.findByEmail(user.getEmail());

        if (existingUserOpt.isPresent()) {
            User existing = existingUserOpt.get();

            // 1. If they are already ACTIVE, this email is genuinely gone.
            if (existing.getStatus() == UserStatus.ACTIVE) {
                throw new IllegalArgumentException("Email is already taken!");
            }

            // 2. ANTI-SPAM: Check if they are clicking too fast
            String limitKey = "LIMIT:OTP_VERIFY:" + existing.getEmail();
            if (Boolean.TRUE.equals(redisTemplate.hasKey(limitKey))) {
                throw new IllegalArgumentException("Please wait 60 seconds before requesting another code.");
            }

            // 3. If they are PENDING, resend the mail
            notificationService.sendVerificationEmail(existing.getEmail());
            return "Verification code resent! Please check your inbox.";
        }

        // 4. New User Logic
        user.setRole(Role.INDEPENDENT_DRIVER);
        user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
        user.setStatus(UserStatus.PENDING_VERIFICATION);
        userRepository.save(user);

        notificationService.sendVerificationEmail(user.getEmail());
        return "User registered! Check your email for the code.";
    }

    @Transactional
    public String verifyEmail(String email, String otp) {
        String redisKey = NotificationService.PREFIX_VERIFY + email;
        String storedOtp = redisTemplate.opsForValue().get(redisKey);

        if (storedOtp == null || !storedOtp.equals(otp)) {
            throw new IllegalArgumentException("Invalid or expired code.");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
        redisTemplate.delete(redisKey);

        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        return jwtUtil.generateToken(userDetails);
    }

    public String login(String email, String password) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        return jwtUtil.generateToken(userDetails);
    }

    public void requestPasswordReset(String email) {
        // Business logic: check if user exists before bothering the mail server
        if (userRepository.existsByEmail(email)) {
            notificationService.sendPasswordResetOtp(email);
        }
        // No exception thrown if missing to prevent user discovery attacks
    }

    @Transactional
    public void resetPassword(String email, String otp, String newPassword) {
        String redisKey = NotificationService.PREFIX_RESET + email;
        String storedOtp = redisTemplate.opsForValue().get(redisKey);

        if (storedOtp == null || !storedOtp.equals(otp)) {
            throw new IllegalArgumentException("Invalid or expired reset code.");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        redisTemplate.delete(redisKey);
    }
}