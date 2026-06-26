package com.yaqazah.user.repository;

import com.yaqazah.report.dto.DriverSessionReportDto;
import com.yaqazah.user.model.Role;
import com.yaqazah.user.model.User;
import org.jspecify.annotations.NullMarked; // Ensure this is imported
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@NullMarked // Tells the compiler: "Everything in here is non-null by default"
public interface UserRepository extends JpaRepository<User, UUID> {

    // The <User> here is now considered non-null
    Optional<User> findByEmail(String email);

    @Query(value = "SELECT * FROM users WHERE email = :email", nativeQuery = true)
    Optional<User> findByEmailIncludingDeleted(@Param("email") String email);

    // Checks if they are the last Admin in the whole system
    long countByRole(Role role);

    // Finds the oldest Company Admin for promotion (assumes a 'createdAt' field exists)
    Optional<User> findFirstByRoleOrderByInsertedAtAsc(Role role);

    // Checks how many admins are left in a specific company
    int countByCompany_CompanyIdAndRole(UUID companyId, Role role);

    List<User> findByCompany_CompanyIdAndRole(UUID companyId, Role role);

    void deleteByCompany_CompanyIdAndRole(UUID companyId, Role role);
    boolean existsByCompany_CompanyIdAndRoleAndIsDeletedFalse(UUID companyId, Role role);
    boolean existsByEmail(String email);

    // Add this inside UserRepository
    List<User> findByIsDeletedTrueAndDeletedAtBefore(Instant cutoffDate);

    int countByCompany_CompanyIdAndRoleIn(UUID companyId, List<Role> roles);
    List<User> findByCompany_CompanyIdAndRoleIn(UUID companyId, List<Role> roles);


//    @Query("SELECT new com.yaqazah.report.dto.DriverSessionReportDto(" +
//            "u.userId, u.fullName, s.sessionId, s.startTime, s.endTime, s.durationHours, s.totalAlerts, " +
//            "d.eventId, d.timestamp, cast(d.type as string), d.severity, d.valueDetected) " +
//            "FROM User u " +
//            "LEFT JOIN Session s ON u.userId = s.user.userId " +
//            "LEFT JOIN DetectionLog d ON s.sessionId = d.session.sessionId " +
//            "WHERE u.company.companyId = :companyId AND u.role = 'DRIVER'")
//        // The compiler expects clarity on the type inside the list
//    List<DriverSessionReportDto> findCombinedDriverDataByCompany(@Param("companyId") UUID companyId);
}