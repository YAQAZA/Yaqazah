package com.yaqazah.user.service;

import com.yaqazah.common.security.JwtUtil;
import com.yaqazah.company.model.Company;
import com.yaqazah.company.service.CompanyService;
import com.yaqazah.infrastructure.email.NotificationService;
import com.yaqazah.user.dto.request.*;
import com.yaqazah.user.dto.response.AuthResponseDto;
import com.yaqazah.user.dto.response.LoginResponseDto;
import com.yaqazah.user.model.RefreshToken;
import com.yaqazah.user.model.Role;
import com.yaqazah.user.model.User;
import com.yaqazah.user.model.UserStatus;
import com.yaqazah.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
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
    private final CompanyService companyService;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public String signup(UserRegistrationDto request) {
        Optional<User> existingUserOpt = userRepository.findByEmail(request.getEmail());

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

        User user = new User();
        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());
        user.setGender(request.getGender());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.INDEPENDENT_DRIVER);
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

        Company newCompany = new Company();
        newCompany.setName(req.getCompanyName());
        newCompany.setAddress(req.getCompanyAddress());

        Company savedCompany = companyService.createCompany(newCompany);

        User newAdmin = new User();
        newAdmin.setEmail(req.getAdminEmail());
        newAdmin.setFullName(req.getAdminFullName());
        newAdmin.setRole(Role.ADMIN);
        newAdmin.setCompany(savedCompany);
        newAdmin.setPasswordHash(passwordEncoder.encode(req.getAdminPassword()));
        newAdmin.setStatus(UserStatus.PENDING_VERIFICATION);

        userRepository.save(newAdmin);

        notificationService.sendVerificationEmail(newAdmin.getEmail());

        return "Company registered successfully. Please check your email for the OTP verification code.";
    }

    @Transactional
    public LoginResponseDto verifyEmail(VerifyEmailDto req) {
        String redisKey = NotificationService.PREFIX_VERIFY + req.getEmail();
        String storedOtp = redisTemplate.opsForValue().get(redisKey);

        // 1. Check if OTP exists
        if (storedOtp == null) {
            throw new IllegalArgumentException("Invalid or expired code.");
        }

        // 2. Secure constant-time equality check to prevent timing attacks
        if (!MessageDigest.isEqual(storedOtp.getBytes(), req.getOtp().getBytes())) {
            throw new IllegalArgumentException("Invalid or expired code.");
        }

        // 3. Delete the OTP immediately to prevent reuse
        redisTemplate.delete(redisKey);

        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        // Generate Access Token
        UserDetails userDetails = userDetailsService.loadUserByUsername(req.getEmail());
        String token = jwtUtil.generateToken(userDetails, req.getClient());

        // Clear existing refresh token to prevent duplicates
        refreshTokenService.deleteByUserId(req.getEmail());

        // Generate Refresh Token
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(req.getEmail());

        AuthResponseDto userDto = new AuthResponseDto(
                user.getEmail(),
                user.getFullName(),
                user.getRole().name(),
                user.getBirthDate()
        );

        return new LoginResponseDto(token, refreshToken.getToken(), userDto);
    }

    @Transactional
    public LoginResponseDto login(LoginRequestDto req) {

        // 1. Fetch User first to check existence and status
        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // 2. Block deleted accounts BEFORE spending CPU cycles on password hashing
        if (user.isDeleted()) {
            throw new DisabledException("Account is scheduled for deletion. Please contact support.");
        }

        // 3. Authenticate credentials
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword())
        );

        // 4. Determine if this request is coming from a mobile app
        boolean isMobile = "mobile".equalsIgnoreCase(req.getClient());

        // 5. THE ENFORCER RULES: Device vs Role
        boolean isDriver = (user.getRole() == Role.INDEPENDENT_DRIVER || user.getRole() == Role.FLEET_DRIVER);
        boolean isAdmin = (user.getRole() == Role.ADMIN || user.getRole() == Role.COMPANY_ADMIN);

        if (isDriver && !isMobile) {
            throw new SecurityException("Drivers must log in using the Mobile App.");
        }

        if (isAdmin && isMobile) {
            throw new SecurityException("Administrators must log in using the Web Dashboard.");
        }

        // 6. Generate Short-lived Access Token
        UserDetails userDetails = userDetailsService.loadUserByUsername(req.getEmail());
        String token = jwtUtil.generateToken(userDetails, req.getClient());

        // Clear existing refresh token before creating a new one
        refreshTokenService.deleteByUserId(req.getEmail());

        // 7. Generate Long-lived Refresh Token
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(req.getEmail());

        AuthResponseDto userDto = new AuthResponseDto(
                user.getEmail(),
                user.getFullName(),
                user.getRole().name(),
                user.getBirthDate()
        );

        return new LoginResponseDto(token, refreshToken.getToken(), userDto);
    }

    public void requestPasswordReset(ForgotPasswordDto req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            notificationService.sendPasswordResetOtp(req.getEmail());
        }
    }

    @Transactional
    public void resetPassword(ResetPasswordDto req) {

        String redisKey = NotificationService.PREFIX_RESET + req.getEmail();
        String storedOtp = redisTemplate.opsForValue().get(redisKey);

        // 1. Check if OTP exists in Redis
        if (storedOtp == null) {
            throw new IllegalArgumentException("Invalid or expired reset code.");
        }

        // 2. Secure constant-time equality check to prevent timing attacks
        if (!MessageDigest.isEqual(storedOtp.getBytes(), req.getOtp().getBytes())) {
            throw new IllegalArgumentException("Invalid or expired reset code.");
        }

        // 3. Delete immediately
        redisTemplate.delete(redisKey);

        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(user);
    }

    @Transactional
    public String refreshAccessToken(String refreshToken, String client) {
        return refreshTokenService.findByToken(refreshToken)
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getUser)
                .map(user -> {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
                    return jwtUtil.generateToken(userDetails, client);
                })
                .orElseThrow(() -> new SecurityException("Refresh token is not in database or is invalid!"));
    }
}