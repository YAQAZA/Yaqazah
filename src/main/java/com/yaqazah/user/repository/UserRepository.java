package com.yaqazah.user.repository;

import com.yaqazah.report.dto.DriverSessionReportDto;
import com.yaqazah.user.model.Role;
import com.yaqazah.user.model.User;
import org.jspecify.annotations.NullMarked; // Ensure this is imported
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@NullMarked // Tells the compiler: "Everything in here is non-null by default"
public interface UserRepository extends JpaRepository<User, UUID> {

    // The <User> here is now considered non-null
    Optional<User> findByEmail(String email);

    int countByCompany_CompanyIdAndRole(UUID companyId, Role role);

    void deleteByCompany_CompanyIdAndRole(UUID companyId, Role role);

    boolean existsByEmail(String email);

    @Query("SELECT new com.yaqazah.report.dto.DriverSessionReportDto(" +
            "u.userId, u.fullName, s.sessionId, s.startDateTime, s.endDateTime, s.durationHours, s.totalAlerts, " +
            "d.eventId, d.timestamp, d.alertId, d.riskId, d.title, d.subtitle) " +
            "FROM User u " +
            "LEFT JOIN Session s ON u.userId = s.userId " +
            "LEFT JOIN DetectionLog d ON s.sessionId = d.session.sessionId " +
            "WHERE u.company.companyId = :companyId AND u.role = com.yaqazah.user.model.Role.FLEET_DRIVER")
    List<DriverSessionReportDto> findCombinedDriverDataByCompany(@Param("companyId") UUID companyId);
}