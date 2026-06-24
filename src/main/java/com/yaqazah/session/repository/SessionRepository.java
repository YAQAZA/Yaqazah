package com.yaqazah.session.repository;

import com.yaqazah.session.model.Session;
import com.yaqazah.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SessionRepository extends JpaRepository<Session, UUID> {
    List<Session> findByUserId(UUID driverId);

    List<Session> findByStartTimeStartingWith(String date);
    // Spring will now delete sessions by passing in the actual User object
    void deleteByUser(User user);

    // Or, if you still want to search using just the UUID, use an underscore
// to tell Spring to look inside the 'user' object for its 'userId'
    List<Session> findByUser_UserId(UUID userId);

}