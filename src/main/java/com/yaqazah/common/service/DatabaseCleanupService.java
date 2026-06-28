package com.yaqazah.common.service;

import com.yaqazah.user.model.User;
import com.yaqazah.user.repository.UserRepository;
import com.yaqazah.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DatabaseCleanupService {

    private final UserRepository userRepository;
    private final UserService userService; // To reuse your exact deletion logic

    // Runs every day at 3:00 AM
    @Scheduled(cron = "0 0 3 * * ?")
    public void hardDeleteOldGarbage() {
        // Calculate the date 30 days ago from exactly right now
        Instant thirtyDaysAgo = Instant.now().minus(30, ChronoUnit.DAYS);

        // Fetch all users who have isDeleted = true AND a deletedAt older than 30 days
        List<User> usersToDelete = userRepository.findByIsDeletedTrueAndDeletedAtBefore(thirtyDaysAgo);

        // Run your existing hard delete logic
        for (User user : usersToDelete) {
            // Note: You will need to rename your existing UserService.deleteAccount method
            // to hardDeleteAccount() so we can use it here.
            userService.hardDeleteAccount(user.getUserId());
        }
    }
}