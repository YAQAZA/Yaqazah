package com.yaqazah.user.repository;

import com.yaqazah.report.dto.DriverSessionReportDto;
import com.yaqazah.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    // Find by email for login and reset password
    Optional<User> findByEmail(String email);

    // Search for drivers by username for the Admin Web Portal
    Optional<User> findByUsername(String username);

    @Query("SELECT new com.yaqazah.report.dto.DriverSessionReportDto(" +
            "u.userId, u.fullName, s.sessionId, s.startTime, s.endTime, s.durationHours, s.totalAlerts, " +
            "d.eventId, d.timestamp, cast(d.type as string), d.severity, d.valueDetected) " +
            "FROM User u " +
            "JOIN Session s ON u.userId = s.userId " +
            "LEFT JOIN DetectionLog d ON s.sessionId = d.sessionId " +
            "WHERE u.companyId = :companyId AND u.role = 'DRIVER'")
    List<DriverSessionReportDto> findCombinedDriverDataByCompany(@Param("companyId") UUID companyId);
}