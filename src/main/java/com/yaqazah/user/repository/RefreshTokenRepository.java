package com.yaqazah.user.repository;

import com.yaqazah.user.model.RefreshToken;
import com.yaqazah.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
    void deleteByUser(User user); // Useful for logging a user out completely
}