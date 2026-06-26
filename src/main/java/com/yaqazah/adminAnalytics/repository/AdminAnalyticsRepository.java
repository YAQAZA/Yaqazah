package com.yaqazah.adminAnalytics.repository;

import com.yaqazah.detection.model.DetectionLog;
import com.yaqazah.user.model.Role;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AdminAnalyticsRepository extends JpaRepository<DetectionLog, UUID> {

    @Query("""
            select s.sessionId, u.fullName, u.userId, s.startTime, s.endTime, s.durationHours, s.totalAlerts
            from Session s
            join User u on s.user.userId = u.userId
            where u.company.companyId = :companyId
              and u.role = :driverRole
              and s.startTime >= :startIso
              and s.startTime < :endIsoExclusive
            order by s.startTime desc
            """)
    List<Object[]> findSessionsForCompanyInPeriod(
            @Param("companyId") UUID companyId,
            @Param("driverRole") Role driverRole,
            @Param("startIso") String startIso,
            @Param("endIsoExclusive") String endIsoExclusive);

//    @Query("""
//            select s.sessionId, u.fullName, u.userId, s.startTime, s.endTime, s.durationHours, s.totalAlerts
//            from Session s
//            join User u on s.userId = u.userId
//            where s.sessionId = :sessionId
//              and u.company.companyId = :companyId
//              and u.role = :driverRole
//            """)
//    Optional<Object[]> findSessionForCompany(
//            @Param("sessionId") UUID sessionId,
//            @Param("companyId") UUID companyId,
//            @Param("driverRole") Role driverRole);
@Query("""
        select s.sessionId, u.fullName, u.userId, s.startTime, s.endTime, s.durationHours, s.totalAlerts
        from Session s
        join User u on s.user.userId = u.userId
        where s.sessionId = :sessionId
          and u.company.companyId = :companyId
          and u.role = :driverRole
        """)
Optional<Object> findSessionForCompany( // <-- Changed to Optional<Object>
                                        @Param("sessionId") UUID sessionId,
                                        @Param("companyId") UUID companyId,
                                        @Param("driverRole") Role driverRole);
    @Query("""
            select d.eventId, d.timestamp, d.type, d.severity, d.valueDetected, d.snapshotUrl
            from DetectionLog d
            where d.session.sessionId = :sessionId
            order by d.timestamp asc
            """)
    List<Object[]> findLogsForSession(@Param("sessionId") UUID sessionId);

    @Query("""
            select d.type, count(d)
            from DetectionLog d
            where d.session.sessionId = :sessionId
            group by d.type
            """)
    List<Object[]> countAlertsByTypeForSession(@Param("sessionId") UUID sessionId);

    @Query("""
            select u.userId, u.fullName, u.email, u.status, u.insertedAt
            from User u
            where u.company.companyId = :companyId
              and u.role = :driverRole
            order by u.fullName asc
            """)
    List<Object[]> findFleetDriversForCompany(
            @Param("companyId") UUID companyId,
            @Param("driverRole") Role driverRole);

    @Query("""
            select u.userId, u.fullName, u.email, u.status, u.insertedAt
            from User u
            where u.userId = :driverId
              and u.company.companyId = :companyId
              and u.role = :driverRole
            """)
    Optional<Object[]> findDriverForCompany(
            @Param("driverId") UUID driverId,
            @Param("companyId") UUID companyId,
            @Param("driverRole") Role driverRole);

    @Query("""
            select count(s)
            from Session s
            where s.user.userId = :driverId
            """)
    long countSessionsForDriver(@Param("driverId") UUID driverId);

    @Query("""
            select s.startTime
            from Session s
            where s.user.userId = :driverId
            order by s.startTime desc
            """)
    List<String> findLastSessionStartTime(@Param("driverId") UUID driverId, Pageable pageable);

    @Query("""
            select s.sessionId, s.startTime, s.durationHours, s.totalAlerts
            from Session s
            where s.user.userId = :driverId
            order by s.startTime desc
            """)
    List<Object[]> findSessionsForDriver(@Param("driverId") UUID driverId);

    @Query("""
            select s.sessionId, s.startTime, s.durationHours, s.totalAlerts
            from Session s
            where s.user.userId = :driverId
              and s.startTime >= :startIso
              and s.startTime < :endIsoExclusive
            order by s.startTime desc
            """)
    List<Object[]> findSessionsForDriverInPeriod(
            @Param("driverId") UUID driverId,
            @Param("startIso") String startIso,
            @Param("endIsoExclusive") String endIsoExclusive);

    @Query("""
            select coalesce(sum(s.durationHours), 0)
            from Session s
            join User u on s.user.userId = u.userId
            where u.company.companyId = :companyId
              and u.role = :driverRole
              and s.startTime >= :startIso
              and s.startTime < :endIsoExclusive
            """)
    double sumDrivingHoursForCompany(
            @Param("companyId") UUID companyId,
            @Param("driverRole") Role driverRole,
            @Param("startIso") String startIso,
            @Param("endIsoExclusive") String endIsoExclusive);

    @Query("""
            select coalesce(sum(s.durationHours), 0)
            from Session s
            where s.user.userId = :driverId
              and s.startTime >= :startIso
              and s.startTime < :endIsoExclusive
            """)
    double sumDrivingHoursForDriver(
            @Param("driverId") UUID driverId,
            @Param("startIso") String startIso,
            @Param("endIsoExclusive") String endIsoExclusive);

    @Query("""
            select coalesce(sum(case when upper(d.severity) = 'HIGH' then 1 else 0 end), 0),
                   coalesce(sum(case when upper(d.severity) = 'LOW' then 1 else 0 end), 0)
            from DetectionLog d
            where d.session.sessionId = :sessionId
            """)
    Object[] sumHighLowForSession(@Param("sessionId") UUID sessionId);

    @Query("""
            select u.userId,
                   sum(case when upper(d.severity) = 'HIGH' then 1 else 0 end),
                   sum(case when upper(d.severity) = 'LOW' then 1 else 0 end)
            from DetectionLog d
            join d.session s
            join d.user u
            where u.company.companyId = :companyId
              and d.timestamp >= :startIso
              and d.timestamp < :endIsoExclusive
              and s.user.userId = u.userId
            group by u.userId
            """)
    List<Object[]> sumHighLowByDriver(
            @Param("companyId") UUID companyId,
            @Param("startIso") String startIso,
            @Param("endIsoExclusive") String endIsoExclusive);

    @Query("""
            select count(distinct s.user.userId)
            from Session s
            join User u on s.user.userId = u.userId
            where u.company.companyId = :companyId
              and u.role = :driverRole
              and s.startTime >= :startIso
              and s.startTime < :endIsoExclusive
            """)
    long countActiveDrivers(
            @Param("companyId") UUID companyId,
            @Param("driverRole") Role driverRole,
            @Param("startIso") String startIso,
            @Param("endIsoExclusive") String endIsoExclusive);

    @Query("""
            select count(s)
            from Session s
            join User u on s.user.userId = u.userId
            where u.company.companyId = :companyId
              and u.role = :driverRole
              and s.startTime >= :startIso
              and s.startTime < :endIsoExclusive
            """)
    long countSessions(
            @Param("companyId") UUID companyId,
            @Param("driverRole") Role driverRole,
            @Param("startIso") String startIso,
            @Param("endIsoExclusive") String endIsoExclusive);

    @Query("""
            select count(d)
            from DetectionLog d
            join d.session s
            join d.user u
            where u.company.companyId = :companyId
              and d.timestamp >= :startIso
              and d.timestamp < :endIsoExclusive
              and s.user.userId = u.userId
            """)
    long countTotalAlerts(
            @Param("companyId") UUID companyId,
            @Param("startIso") String startIso,
            @Param("endIsoExclusive") String endIsoExclusive);

    @Query("""
            select count(d)
            from DetectionLog d
            join d.session s
            where s.user.userId = :driverId
              and d.timestamp >= :startIso
              and d.timestamp < :endIsoExclusive
            """)
    long countAlertsForDriver(
            @Param("driverId") UUID driverId,
            @Param("startIso") String startIso,
            @Param("endIsoExclusive") String endIsoExclusive);

    @Query("""
            select cast(d.timestamp as date), d.type, count(d)
            from DetectionLog d
            join d.session s
            where s.user.userId = :driverId
              and d.timestamp >= :startIso
              and d.timestamp < :endIsoExclusive
            group by cast(d.timestamp as date), d.type
            """)
    List<Object[]> countAlertsByDayAndTypeForDriver(
            @Param("driverId") UUID driverId,
            @Param("startIso") String startIso,
            @Param("endIsoExclusive") String endIsoExclusive);

    @Query("""
            select d.type, count(d)
            from DetectionLog d
            join d.session s
            where s.user.userId = :driverId
              and d.timestamp >= :startIso
              and d.timestamp < :endIsoExclusive
            group by d.type
            """)
    List<Object[]> countAlertsByTypeForDriver(
            @Param("driverId") UUID driverId,
            @Param("startIso") String startIso,
            @Param("endIsoExclusive") String endIsoExclusive);
}
