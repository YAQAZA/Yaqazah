package com.yaqazah.adminAnalytics.service;

import com.yaqazah.adminAnalytics.dto.*;
import com.yaqazah.adminAnalytics.repository.AdminAnalyticsRepository;
import com.yaqazah.dashboard.dto.OverviewStatDto;
import com.yaqazah.dashboard.util.AlertTypeMapper;
import com.yaqazah.dashboard.util.DashboardFilterResolver;
import com.yaqazah.detection.model.DetectionType;
import com.yaqazah.user.model.Role;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
@Service
public class SessionAnalyticsService {

    private static final DateTimeFormatter SESSION_DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter SESSION_TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final AdminAnalyticsRepository repository;

    public SessionAnalyticsService(AdminAnalyticsRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public  SessionsListResponseDto buildSessionsList(UUID companyId, String filter, String fromIso, String toIso) {
        DashboardFilterResolver.DateRange range = DashboardFilterResolver.resolve(filter, fromIso, toIso);
        String curStartIso = startOfDayUtcIso(range.from());
        String curEndExcl = startOfDayUtcIso(range.to().plusDays(1));

        LocalDate[] previous = previousInclusiveRange(range.from(), range.to());
        String prevStartIso = startOfDayUtcIso(previous[0]);
        String prevEndExcl = startOfDayUtcIso(previous[1].plusDays(1));

        long sessionsCur = repository.countSessions(companyId, Role.FLEET_DRIVER, curStartIso, curEndExcl);
        long sessionsPrev = repository.countSessions(companyId, Role.FLEET_DRIVER, prevStartIso, prevEndExcl);
        long activeCur = repository.countActiveDrivers(companyId, Role.FLEET_DRIVER, curStartIso, curEndExcl);
        long activePrev = repository.countActiveDrivers(companyId, Role.FLEET_DRIVER, prevStartIso, prevEndExcl);
        long alertsCur = repository.countTotalAlerts(companyId, curStartIso, curEndExcl);
        long alertsPrev = repository.countTotalAlerts(companyId, prevStartIso, prevEndExcl);
        Double avgScoreCur = companyAverageSafetyScore(companyId, curStartIso, curEndExcl);
        Double avgScorePrev = companyAverageSafetyScore(companyId, prevStartIso, prevEndExcl);

        List<SessionSummaryDto> sessions = new ArrayList<>();
        for (Object[] row : repository.findSessionsForCompanyInPeriod(
                companyId, Role.FLEET_DRIVER, curStartIso, curEndExcl)) {
            sessions.add(mapSessionRow(row, false));
        }

        return SessionsListResponseDto.builder()
                .filterId(DashboardFilterResolver.toFilterId(filter))
                .timeInterval(DashboardFilterResolver.formatTimeInterval(range.from(), range.to()))
                .overviewStats(List.of(
                        overviewStat("Total Sessions", sessionsCur, sessionsPrev, false),
                        overviewStat("Active Drivers", activeCur, activePrev, false),
                        overviewStat("Average Safety Score", avgScoreCur, avgScorePrev, true),
                        overviewStat("Total Alerts", alertsCur, alertsPrev, false)))
                .sessions(sessions)
                .build();
    }

    @Transactional(readOnly = true)

    public Optional<SessionDetailsResponseDto> buildSessionDetails(UUID companyId, UUID sessionId) {
        Optional<Object> sessionRow = repository.findSessionForCompany(sessionId, companyId, Role.FLEET_DRIVER);
        if (sessionRow.isEmpty()) {
            return Optional.empty();
        }
        // Since it's no longer double-wrapped, just cast the single Object to your Object[]

        Object[] row = (Object[]) sessionRow.get();
        UUID sid = (UUID) row[0];
        String driverName = (String) row[1];
        UUID driverId = (UUID) row[2];
        String startTime = row[3] != null ? row[3].toString() : null;
        String endTime = row[4] != null ? row[4].toString() : null;
        float hours = row[5] == null ? 0f : ((Number) row[5]).floatValue();
        int alerts = row[6] == null ? 0 : ((Number) row[6]).intValue();

        int safetyScore = sessionSafetyScore(sid);
        int riskId = riskLevel(safetyScore);

        Map<DetectionType, Long> typeCounts = alertTypeCountsForSession(sid);

        SessionDetailDto session = SessionDetailDto.builder()
                .sessionId(sid.toString())
                .driver(driverName)
                .driverId(driverId.toString())
                .startDateTime(startTime)
                .endDateTime(endTime)
                .duration(formatDurationHours(hours))
                .safetyScore(safetyScore)
                .alertsNumber(alerts)
                .distractionCount(typeCounts.getOrDefault(DetectionType.DISTRACTION, 0L).intValue())
                .drowsyCount(typeCounts.getOrDefault(DetectionType.DROWSINESS, 0L).intValue())
                .sleepCount(typeCounts.getOrDefault(DetectionType.SLEEPINESS, 0L).intValue())
                .riskId(riskId)
                .build();

        List<DetectionLogDto> logs = new ArrayList<>();
        for (Object[] logRow : repository.findLogsForSession(sid)) {
            DetectionType type = (DetectionType) logRow[2];
            Integer typeId = AlertTypeMapper.toTypeId(type);
            logs.add(DetectionLogDto.builder()
                    .eventId(((UUID) logRow[0]).toString())
                    .timestamp((String) logRow[1])
                    .typeId(typeId != null ? typeId : -1)
                    .severity((String) logRow[3])
                    .valueDetected(logRow[4] == null ? 0f : ((Number) logRow[4]).floatValue())
                    .snapshotUrl((String) logRow[5])
                    .build());
        }

        return Optional.of(SessionDetailsResponseDto.builder()
                .session(session)
                .logs(logs)
                .build());
    }


    private int sessionSafetyScore(UUID sessionId) {
        // 1. This is actually returning an array of rows!
        Object[] resultList = repository.sumHighLowForSession(sessionId);

        if (resultList == null || resultList.length == 0) {
            return 100;
        }

        // 2. Extract the actual row (the columns) from the first element
        Object[] columns = (Object[]) resultList[0];

        // 3. Now it is safe to extract and cast the numbers
        long high = columns[0] == null ? 0L : ((Number) columns[0]).longValue();
        long low = columns[1] == null ? 0L : ((Number) columns[1]).longValue();

        return (int) Math.round(individualScore(high, low));
    }

    private Double companyAverageSafetyScore(UUID companyId, String startIso, String endIsoExclusive) {
        Map<UUID, long[]> highLow = highLowByDriver(companyId, startIso, endIsoExclusive);
        long active = repository.countActiveDrivers(companyId, Role.FLEET_DRIVER, startIso, endIsoExclusive);
        if (active == 0) {
            return null;
        }
        double sum = 0.0;
        for (Map.Entry<UUID, long[]> e : highLow.entrySet()) {
            sum += individualScore(e.getValue()[0], e.getValue()[1]);
        }
        long driversWithoutAlerts = active - highLow.size();
        sum += driversWithoutAlerts * 100.0;
        return sum / active;
    }

    private Map<UUID, long[]> highLowByDriver(UUID companyId, String startIso, String endIsoExclusive) {
        Map<UUID, long[]> map = new HashMap<>();
        for (Object[] row : repository.sumHighLowByDriver(companyId, startIso, endIsoExclusive)) {
            UUID userId = (UUID) row[0];
            long high = row[1] == null ? 0L : ((Number) row[1]).longValue();
            long low = row[2] == null ? 0L : ((Number) row[2]).longValue();
            map.put(userId, new long[]{high, low});
        }
        return map;
    }

    private double individualScore(long highCount, long lowCount) {
        return Math.max(0.0, 100.0 - (5.0 * highCount + 2.0 * lowCount));
    }

    private int riskLevel(double score) {
        if (score > 90.0) {
            return 0;
        }
        if (score >= 70.0) {
            return 1;
        }
        return 2;
    }

    private OverviewStatDto overviewStat(String label, long current, long previous, boolean asPercent) {
        return overviewStat(label, (double) current, (double) previous, asPercent, false);
    }

    private OverviewStatDto overviewStat(String label, Double current, Double previous, boolean asPercent) {
        return overviewStat(label, current, previous, asPercent, false);
    }

    private OverviewStatDto overviewStat(
            String label, Double current, Double previous, boolean asPercent, boolean asTime) {

        // 1. Safe handling for the current value (default to 0.0 if null)
        double safeCurrent = (current != null) ? current : 0.0;

        // 2. Determine the delta string safely
        String deltaStr;
        if (previous == null || previous == 0.0) {
            if (safeCurrent == 0.0) {
                deltaStr = formatDeltaPercent(0.0); // Both are 0/null -> 0% change
            } else {
                deltaStr = "N/A"; // Or "+100%", "New", etc., depending on your UI preference
            }
        } else {
            // Safe to calculate since previous is neither null nor zero
            deltaStr = formatDeltaPercent(deltaPercent(safeCurrent, previous));
        }

        return OverviewStatDto.builder()
                .label(label)
                .value(formatOverviewValue(safeCurrent, asPercent, asTime))
                .delta(deltaStr)
                .build();
    }

    private String formatOverviewValue(double value, boolean asPercent, boolean asTime) {
        if (asTime) {
            return formatDrivingHours(value);
        }
        if (asPercent) {
            return Math.round(value) + "%";
        }
        return String.valueOf(Math.round(value));
    }

    private String formatDrivingHours(double hours) {
        int totalSeconds = (int) Math.round(hours * 3600);
        int h = totalSeconds / 3600;
        int m = (totalSeconds % 3600) / 60;
        int s = totalSeconds % 60;
        return String.format(Locale.US, "%d:%02d:%02d", h, m, s);
    }

    private Double deltaPercent(double current, double previous) {
        if (previous == 0.0) {
            return current == 0.0 ? 0.0 : 100.0;
        }
        return ((current - previous) / previous) * 100.0;
    }

    private String formatDeltaPercent(double delta) {
        if (delta == 0.0) {
            return "0%";
        }
        String sign = delta > 0 ? "+" : "";
        if (Math.abs(delta - Math.round(delta)) < 0.05) {
            return sign + Math.round(delta) + "%";
        }
        return sign + String.format(Locale.US, "%.1f", delta) + "%";
    }

    private String formatDurationHours(float durationHours) {
        int totalMinutes = Math.round(durationHours * 60);
        int hours = totalMinutes / 60;
        int minutes = totalMinutes % 60;
        return hours + "h " + minutes + "m";
    }

    private LocalDate[] previousInclusiveRange(LocalDate fromDate, LocalDate toDate) {
        long inclusiveDays = ChronoUnit.DAYS.between(fromDate, toDate) + 1;
        LocalDate previousEnd = fromDate.minusDays(1);
        LocalDate previousStart = previousEnd.minusDays(inclusiveDays - 1);
        return new LocalDate[]{previousStart, previousEnd};
    }

    private String startOfDayUtcIso(LocalDate day) {
        return day.atStartOfDay(ZoneOffset.UTC).toInstant().toString();
    }

    private SessionSummaryDto mapSessionRow(Object[] row, boolean splitDateTime) {

        // 1. Extract standard fields
        UUID sessionId = (UUID) row[0];
        String driverName = (String) row[1];
        UUID driverId = (UUID) row[2];
        String startTime = row[3] != null ? row[3].toString() : null;
        // 2. Safely cast numeric fields to prevent ClassCastException
        float hours = row[5] == null ? 0f : ((Number) row[5]).floatValue();
        int alerts = row[6] == null ? 0 : ((Number) row[6]).intValue();

        // 3. Calculate derived data
        int score = sessionSafetyScore(sessionId);

        // 4. Build the base DTO
        SessionSummaryDto.SessionSummaryDtoBuilder builder = SessionSummaryDto.builder()
                .sessionId(sessionId != null ? sessionId.toString() : null)
                .driver(driverName)
                .driverId(driverId != null ? driverId.toString() : null)
                .startDateTime(startTime)
                .duration(formatDurationHours(hours))
                .safetyScore(score)
                .alertsNumber(alerts)
                .riskId(riskLevel(score));

        // 5. Handle optional date/time splitting safely
        if (splitDateTime && startTime != null) {
            builder.startDate(extractDate(startTime));
            builder.startTime(extractTime(startTime));
        }

        return builder.build();
    }
    private Map<DetectionType, Long> alertTypeCountsForSession(UUID sessionId) {
        Map<DetectionType, Long> map = new HashMap<>();
        for (Object[] row : repository.countAlertsByTypeForSession(sessionId)) {
            map.put((DetectionType) row[0], ((Number) row[1]).longValue());
        }
        return map;
    }
    private String extractDate(String isoTimestamp) {
        try {
            return Instant.parse(isoTimestamp).atZone(ZoneOffset.UTC).format(SESSION_DATE_FMT);
        } catch (Exception ex) {
            return isoTimestamp.length() >= 10 ? isoTimestamp.substring(0, 10) : isoTimestamp;
        }
    }
    private String extractTime(String isoTimestamp) {
        try {
            return Instant.parse(isoTimestamp).atZone(ZoneOffset.UTC).format(SESSION_TIME_FMT);
        } catch (Exception ex) {
            return "";
        }
    }
}
