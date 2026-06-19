package com.yaqazah.user.service;

import com.yaqazah.common.security.JwtUtil;
import com.yaqazah.company.model.Company;
import com.yaqazah.company.service.CompanyService;
import com.yaqazah.infrastructure.email.NotificationService;
import com.yaqazah.user.dto.CompanyOwnerRegistrationDto;
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
    private final CompanyService companyService; // Added to handle company creation

    @Transactional
    public String signup(User user) {
        Optional<User> existingUserOpt = userRepository.findByEmail(user.getEmail());

        if (existingUserOpt.isPresent()) {
            User existing = existingUserOpt.get();

            if (existing.getStatus() == UserStatus.ACTIVE) {
                throw new IllegalArgumentException("Email is already taken!");
            }

            String limitKey = "LIMIT:OTP_VERIFY:" + existing.getEmail();
            if (Boolean.TRUE.equals(redisTemplate.hasKey(limitKey))) {
                throw new IllegalArgumentException("Please wait 60 seconds before requesting another code.");
            }

            notificationService.sendVerificationEmail(existing.getEmail());
            return "Verification code resent! Please check your inbox.";
        }

        user.setRole(Role.INDEPENDENT_DRIVER);
        user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
        user.setStatus(UserStatus.PENDING_VERIFICATION);
        userRepository.save(user);

        notificationService.sendVerificationEmail(user.getEmail());
        return "User registered! Check your email for the code.";
    }

    @Transactional
    public String registerCompanyOwner(CompanyOwnerRegistrationDto req) {
        Optional<User> existingUserOpt = userRepository.findByEmail(req.getAdminEmail());

        if (existingUserOpt.isPresent()) {
            User existing = existingUserOpt.get();

            // 1. If active, block the request
            if (existing.getStatus() == UserStatus.ACTIVE) {
                throw new IllegalArgumentException("Email is already taken!");
            }

            // 2. Anti-spam check
            String limitKey = "LIMIT:OTP_VERIFY:" + existing.getEmail();
            if (Boolean.TRUE.equals(redisTemplate.hasKey(limitKey))) {
                throw new IllegalArgumentException("Please wait 60 seconds before requesting another code.");
            }

            // 3. If pending, resend OTP (Assume company was already created in the first attempt)
            notificationService.sendVerificationEmail(existing.getEmail());
            return "Verification code resent! Please check your inbox.";
        }

        // 4. Fresh Registration: Create the Company
        Company newCompany = new Company();
        newCompany.setName(req.getCompanyName());
        newCompany.setAddress(req.getCompanyAddress());

        Company savedCompany = companyService.createCompany(newCompany);

        // 5. Create the Admin and link them to the newly created company
        User newAdmin = new User();
        newAdmin.setEmail(req.getAdminEmail());
        newAdmin.setFullName(req.getAdminFullName());
        newAdmin.setRole(Role.ADMIN);
        newAdmin.setCompany(savedCompany);
        newAdmin.setPasswordHash(passwordEncoder.encode(req.getAdminPassword()));

        // Ensure status is PENDING_VERIFICATION for the OTP flow
        newAdmin.setStatus(UserStatus.PENDING_VERIFICATION);

        userRepository.save(newAdmin);

        // 6. Send the standard OTP verification email
        notificationService.sendVerificationEmail(newAdmin.getEmail());

        return "Company registered successfully. Please check your email for the OTP verification code.";
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
        if (userRepository.existsByEmail(email)) {
            notificationService.sendPasswordResetOtp(email);
        }
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