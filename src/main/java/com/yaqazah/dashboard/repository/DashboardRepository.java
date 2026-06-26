package com.yaqazah.dashboard.repository;

import com.yaqazah.detection.model.DetectionLog;
import com.yaqazah.user.model.Role;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * Read-only analytics queries for the dashboard. Uses JPQL with string bounds for ISO timestamps.
 */
public interface DashboardRepository extends JpaRepository<DetectionLog, UUID> {

//    @Query("""
//            select count(distinct s.userId)
//            from Session s
//            join User u on s.userId = u.userId
//            where u.company.companyId = :companyId
//              and u.role = :driverRole
//              and s.startTime >= :startIso
//              and s.startTime < :endIsoExclusive
//            """)
//    long countActiveDrivers(
//            @Param("companyId") UUID companyId,
//            @Param("driverRole") Role driverRole,
//            @Param("startIso") String startIso,
//            @Param("endIsoExclusive") String endIsoExclusive);

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
            select s.user.userId, count(s)
            from Session s
            join User u on s.user.userId = u.userId
            where u.company.companyId = :companyId
              and u.role = :driverRole
              and s.startTime >= :startIso
              and s.startTime < :endIsoExclusive
            group by s.user.userId
            """)
    List<Object[]> countSessionsByDriver(
            @Param("companyId") UUID companyId,
            @Param("driverRole") Role driverRole,
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
            select distinct s.user.userId
            from Session s
            join User u on s.user.userId = u.userId
            where u.company.companyId = :companyId
              and u.role = :driverRole
              and s.startTime >= :startIso
              and s.startTime < :endIsoExclusive
            """)
    List<UUID> findActiveDriverIdsInPeriod(
            @Param("companyId") UUID companyId,
            @Param("driverRole") Role driverRole,
            @Param("startIso") String startIso,
            @Param("endIsoExclusive") String endIsoExclusive);

//    @Query("""
//            select u.userId,
//                   sum(case when upper(d.severity) = 'HIGH' then 1 else 0 end),
//                   sum(case when upper(d.severity) = 'LOW' then 1 else 0 end)
//            from DetectionLog d
//            join d.session s
//            join d.user u
//            where u.company.companyId = :companyId
//              and d.timestamp >= :startIso
//              and d.timestamp < :endIsoExclusive
//              and s.userId = u.userId
//            group by u.userId
//            """)
//    List<Object[]> sumHighLowByDriver(
//            @Param("companyId") UUID companyId,
//            @Param("startIso") String startIso,
//            @Param("endIsoExclusive") String endIsoExclusive);

    @Query("""
            select cast(d.timestamp as date), d.type, count(d)
            from DetectionLog d
            join d.session s
            join d.user u
            where u.company.companyId = :companyId
              and d.timestamp >= :trendStartIso
              and d.timestamp < :endIsoExclusive
              and s.user.userId = u.userId
            group by cast(d.timestamp as date), d.type
            """)
    List<Object[]> countAlertsByDayAndType(
            @Param("companyId") UUID companyId,
            @Param("trendStartIso") String trendStartIso,
            @Param("endIsoExclusive") String endIsoExclusive);

    @Query("""
            select d.type, count(d)
            from DetectionLog d
            join d.session s
            join d.user u
            where u.company.companyId = :companyId
              and d.timestamp >= :startIso
              and d.timestamp < :endIsoExclusive
              and s.user.userId = u.userId
            group by d.type
            """)
    List<Object[]> countAlertsByType(
            @Param("companyId") UUID companyId,
            @Param("startIso") String startIso,
            @Param("endIsoExclusive") String endIsoExclusive);

    @Query("""
            select s.sessionId, u.fullName, s.durationHours, s.totalAlerts
            from Session s
            join User u on s.user.userId = u.userId
            where u.company.companyId = :companyId
              and u.role = :driverRole
            order by s.startTime desc
            """)
    List<Object[]> findRecentSessionsForCompany(
            @Param("companyId") UUID companyId,
            @Param("driverRole") Role driverRole,
            Pageable pageable);

    @Query("""
            select u.userId, u.fullName
            from User u
            where u.userId in :userIds
            """)
    List<Object[]> findDriverNamesByIds(@Param("userIds") List<UUID> userIds);


    // 1. Get all drivers for a company
    @Query("""
            select u.userId, u.fullName, u.email, u.status, u.insertedAt
            from User u
            where u.company.companyId = :companyId
              and u.role = :role
            order by u.fullName asc
            """)
    List<Object[]> findFleetDriversForCompany(
            @Param("companyId") UUID companyId,
            @Param("role") Role role);

    // 2. Count distinct active drivers in a specific time period
    @Query("""
            select count(distinct s.user.userId)
            from Session s
            join User u on s.user.userId = u.userId
            where u.company.companyId = :companyId
              and u.role = :role
              and s.startTime >= :startIso
              and s.startTime < :endIso
            """)
    long countActiveDrivers(
            @Param("companyId") UUID companyId,
            @Param("role") Role role,
            @Param("startIso") String startIso,
            @Param("endIso") String endIso);

    // 3. Sum High/Low alerts per driver for the safety score calculation
    @Query("""
            select u.userId,
                   coalesce(sum(case when upper(d.severity) = 'HIGH' then 1 else 0 end), 0),
                   coalesce(sum(case when upper(d.severity) = 'LOW' then 1 else 0 end), 0)
            from User u
            left join Session s on u.userId = s.user.userId 
                and s.startTime >= :startIso and s.startTime < :endIso
            left join DetectionLog d on s.sessionId = d.session.sessionId
            where u.company.companyId = :companyId
            group by u.userId
            """)
    List<Object[]> sumHighLowByDriver(
            @Param("companyId") UUID companyId,
            @Param("startIso") String startIso,
            @Param("endIso") String endIso);

    // 4. Fetch basic session stats for building performance and alert trends
    @Query("""
            select s.sessionId, s.startTime, s.durationHours, s.totalAlerts
            from Session s
            where s.user.userId = :driverId
            order by s.startTime desc
            """)
    List<Object[]> findSessionsForDriver(@Param("driverId") UUID driverId);

    // 5. Get the most recent session start time for "Last Active" status
    @Query("""
            select s.startTime 
            from Session s 
            where s.user.userId = :driverId 
            order by s.startTime desc
            """)
    List<String> findLastSessionStartTime(
            @Param("driverId") UUID driverId,
            Pageable pageable);
}

