package com.yaqazah.user.service;

import com.yaqazah.user.model.RefreshToken;
import com.yaqazah.user.repository.RefreshTokenRepository;
import com.yaqazah.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    // Set refresh token expiration (e.g., 7 days)
    private final long REFRESH_TOKEN_EXPIRATION_MS = 7 * 24 * 60 * 60 * 1000L;

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    @Transactional
    public RefreshToken createRefreshToken(String email) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Delete any existing token for this user so they don't pile up
        refreshTokenRepository.deleteByUser(user);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(UUID.randomUUID().toString()); // Secure random string
        refreshToken.setExpiryDate(Instant.now().plusMillis(REFRESH_TOKEN_EXPIRATION_MS));

        return refreshTokenRepository.save(refreshToken);
    }

    public RefreshToken verifyExpiration(RefreshToken token) {
        // 1. Check if it's already dead
        if (token.getExpiryDate().compareTo(Instant.now()) < 0) {
            refreshTokenRepository.delete(token);
            throw new SecurityException("Refresh token was expired. Please make a new signin request");
        }

        // 2. THE SLIDING WINDOW: It is valid, so let's extend its life!
        // Push the expiration date back another 7 days from RIGHT NOW
        token.setExpiryDate(Instant.now().plusMillis(REFRESH_TOKEN_EXPIRATION_MS));

        // 3. Save the updated date back to the database
        return refreshTokenRepository.save(token);
    }

    @Transactional
    public void deleteByUserId(String email) {
        var user = userRepository.findByEmail(email).orElseThrow();
        refreshTokenRepository.deleteByUser(user);
    }
}