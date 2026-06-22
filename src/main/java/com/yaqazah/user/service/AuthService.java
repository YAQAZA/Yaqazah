package com.yaqazah.user.service;

import com.yaqazah.common.security.JwtUtil;
import com.yaqazah.company.model.Company;
import com.yaqazah.company.service.CompanyService;
import com.yaqazah.infrastructure.email.NotificationService;
import com.yaqazah.user.dto.AuthResponseDto;
import com.yaqazah.user.dto.CompanyOwnerRegistrationDto;
import com.yaqazah.user.dto.LoginResponseDto;
import com.yaqazah.user.dto.UserRegistrationDto;
import com.yaqazah.user.model.RefreshToken;
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
    public LoginResponseDto verifyEmail(String email, String otp, String client) {
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

        // Generate Access Token
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        String token = jwtUtil.generateToken(userDetails, client);

        // Clear existing refresh token to prevent duplicates
        refreshTokenService.deleteByUserId(email);

        // Generate Refresh Token
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(email);

        AuthResponseDto userDto = new AuthResponseDto(
                user.getEmail(),
                user.getFullName(),
                user.getRole().name()
        );

        // Return both tokens
        return new LoginResponseDto(token, refreshToken.getToken(), userDto);
    }

    @Transactional
    public LoginResponseDto login(String email, String password, String client) {

        // 1. Authenticate credentials
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));

        // 2. Fetch User to check roles
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // 3. Determine if this request is coming from a mobile app
        boolean isMobile = "mobile".equalsIgnoreCase(client);

        // 4. THE ENFORCER RULES: Device vs Role
        boolean isDriver = (user.getRole() == Role.INDEPENDENT_DRIVER || user.getRole() == Role.FLEET_DRIVER);
        boolean isAdmin = (user.getRole() == Role.ADMIN || user.getRole() == Role.COMPANY_ADMIN);

        if (isDriver && !isMobile) {
            throw new SecurityException("Drivers must log in using the Mobile App.");
        }

        if (isAdmin && isMobile) {
            throw new SecurityException("Administrators must log in using the Web Dashboard.");
        }

        // 5. Generate Short-lived Access Token
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        String token = jwtUtil.generateToken(userDetails, client);

        // --- THE FIX: Clear existing refresh token before creating a new one ---
        refreshTokenService.deleteByUserId(email);

        // 6. Generate Long-lived Refresh Token
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(email);

        AuthResponseDto userDto = new AuthResponseDto(
                user.getEmail(),
                user.getFullName(),
                user.getRole().name()
        );

        // 7. Return both tokens
        return new LoginResponseDto(token, refreshToken.getToken(), userDto);
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

    @Transactional
    public String refreshAccessToken(String refreshToken, String client) {
        return refreshTokenService.findByToken(refreshToken)
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getUser)
                .map(user -> {
                    // Generate and return the new Access Token
                    UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
                    return jwtUtil.generateToken(userDetails, client);
                })
                .orElseThrow(() -> new SecurityException("Refresh token is not in database or is invalid!"));
    }
}