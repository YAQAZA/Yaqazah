package com.yaqazah.adminAnalytics.service;

import com.yaqazah.adminAnalytics.repository.AdminAnalyticsRepository;
import com.yaqazah.adminAnalytics.dto.DriverDetailResponseDto;
import com.yaqazah.adminAnalytics.dto.DriverSummaryDto;
import com.yaqazah.adminAnalytics.dto.DriversListResponseDto;
import com.yaqazah.adminAnalytics.dto.SessionSummaryDto;
import com.yaqazah.dashboard.dto.AlertTrendValueDto;
import com.yaqazah.dashboard.dto.OverviewStatDto;
import com.yaqazah.dashboard.dto.PieDistributionDto;
import com.yaqazah.dashboard.dto.RiskDistributionDto;
import com.yaqazah.dashboard.util.AlertTypeMapper;
import com.yaqazah.dashboard.util.DashboardFilterResolver;
import com.yaqazah.dashboard.util.DashboardFilterResolver.DateRange;
import com.yaqazah.detection.model.DetectionType;
import com.yaqazah.user.model.Role;
import com.yaqazah.user.model.UserStatus;
import com.yaqazah.user.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class DriversAnalyticsService {

    private static final DateTimeFormatter TREND_DAY_KEY = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter JOINED_AT_FMT = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.US);
    private static final DateTimeFormatter SESSION_DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter SESSION_TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final int TREND_DAYS = 7;
    private static final int DRIVER_LIST_TREND_POINTS = 4;

    private final AdminAnalyticsRepository repository;
    private final UserRepository userRepository;

    public DriversAnalyticsService(AdminAnalyticsRepository repository, UserRepository userRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
    }

@Transactional(readOnly = true)
public DriversListResponseDto buildDriversList(UUID companyId, String filter, String fromIso, String toIso) {

    // 1. Resolve date ranges for current and previous periods
    DashboardFilterResolver.DateRange range = DashboardFilterResolver.resolve(filter, fromIso, toIso);
    String curStartIso = startOfDayUtcIso(range.from());
    String curEndExcl = startOfDayUtcIso(range.to().plusDays(1));

    LocalDate[] previous = previousInclusiveRange(range.from(), range.to());
    String prevStartIso = startOfDayUtcIso(previous[0]);
    String prevEndExcl = startOfDayUtcIso(previous[1].plusDays(1));

    // 2. Calculate Top-Level Overview Stats
    long totalDrivers = userRepository.countByCompany_CompanyIdAndRole(companyId, Role.FLEET_DRIVER);
    long activeCur = repository.countActiveDrivers(companyId, Role.FLEET_DRIVER, curStartIso, curEndExcl);
    long activePrev = repository.countActiveDrivers(companyId, Role.FLEET_DRIVER, prevStartIso, prevEndExcl);
    Double avgScoreCur = companyAverageSafetyScore(companyId, curStartIso, curEndExcl);
    Double avgScorePrev = companyAverageSafetyScore(companyId, prevStartIso, prevEndExcl);

    // 3. Fetch and map all drivers for the company
    List<DriverSummaryDto> drivers = new ArrayList<>();
    for (Object[] row : repository.findFleetDriversForCompany(companyId, Role.FLEET_DRIVER)) {
        drivers.add(mapDriverRow(row, companyId, curStartIso, curEndExcl));
    }

    // 4. Calculate Risk counts
    long highRiskCur = drivers.stream().filter(d -> d.getRiskId() == 2).count();
    long highRiskPrev = countHighRiskDrivers(companyId, prevStartIso, prevEndExcl);

    // 5. Resolve the selected driver (defaults to the first one in the list)
    DriverSummaryDto selected = drivers.isEmpty() ? null : drivers.get(0);

    // 6. Build Trends for the selected driver
    List<Integer> performanceTrend = List.of();
    List<Integer> alertTrendValues = List.of();

    if (selected != null) {
        UUID driverUuid = UUID.fromString(selected.getId());
        performanceTrend = buildDriverPerformanceTrend(driverUuid, DRIVER_LIST_TREND_POINTS);
        alertTrendValues = buildDriverAlertTrendSimple(driverUuid, DRIVER_LIST_TREND_POINTS);
    }

    // 7. Assemble the final DTO
    return DriversListResponseDto.builder()
            .filterId(DashboardFilterResolver.toFilterId(filter))
            .performanceTrend(performanceTrend)
            .alertTrendValues(alertTrendValues)
            .overviewStats(List.of(
                    overviewStat("Total Drivers", totalDrivers, totalDrivers, false),
                    overviewStat("Active Drivers", activeCur, activePrev, false),
                    overviewStat("Average Safety Score", avgScoreCur, avgScorePrev, true),
                    overviewStat("High Risk Drivers", highRiskCur, highRiskPrev, false)))
            .drivers(drivers)
            .build();
}
    @Transactional(readOnly = true)
    public Optional<DriverDetailResponseDto> buildDriverDetail(
            UUID companyId, UUID driverId, String filter, String fromIso, String toIso) {

        Optional<Object[]> driverRow = repository.findDriverForCompany(driverId, companyId, Role.FLEET_DRIVER);
        if (driverRow.isEmpty()) {
            return Optional.empty();
        }

        DateRange range = DashboardFilterResolver.resolve(filter, fromIso, toIso);
        String curStartIso = startOfDayUtcIso(range.from());
        String curEndExcl = startOfDayUtcIso(range.to().plusDays(1));

        LocalDate[] previous = previousInclusiveRange(range.from(), range.to());
        String prevStartIso = startOfDayUtcIso(previous[0]);
        String prevEndExcl = startOfDayUtcIso(previous[1].plusDays(1));

        List<Object[]> sessionsInPeriod = repository.findSessionsForDriverInPeriod(driverId, curStartIso, curEndExcl);
        List<Object[]> sessionsPrev = repository.findSessionsForDriverInPeriod(driverId, prevStartIso, prevEndExcl);

        long sessionsCur = sessionsInPeriod.size();
        long sessionsPrevLong = sessionsPrev.size();
        long highRiskCur = sessionsInPeriod.stream()
                .mapToLong(r -> riskLevel(sessionSafetyScore((UUID) r[0])) == 2 ? 1 : 0)
                .sum();
        long highRiskPrev = sessionsPrev.stream()
                .mapToLong(r -> riskLevel(sessionSafetyScore((UUID) r[0])) == 2 ? 1 : 0)
                .sum();
        long alertsCur = repository.countAlertsForDriver(driverId, curStartIso, curEndExcl);
        long alertsPrev = repository.countAlertsForDriver(driverId, prevStartIso, prevEndExcl);
        double hoursCur = repository.sumDrivingHoursForDriver(driverId, curStartIso, curEndExcl);
        double hoursPrev = repository.sumDrivingHoursForDriver(driverId, prevStartIso, prevEndExcl);

//        DriverSummaryDto selected = mapDriverRow(driverRow.get(), companyId, curStartIso, curEndExcl);
        // 1. Get the wrapped array from the Optional
        Object[] wrappedRow = driverRow.get();

        // 2. Extract the actual columns from the first index
        Object[] actualRow = (Object[]) wrappedRow[0];

        // 3. Pass the unwrapped row to your mapper
        DriverSummaryDto selected = mapDriverRow(actualRow, companyId, curStartIso, curEndExcl);

        List<SessionSummaryDto> sessions = new ArrayList<>();
        for (Object[] row : sessionsInPeriod) {
            UUID sid = (UUID) row[0];
            String startTime = row[1] != null ? row[1].toString() : null;
            float hours = row[2] == null ? 0f : ((Number) row[2]).floatValue();
            int alerts = row[3] == null ? 0 : ((Number) row[3]).intValue();
            int score = sessionSafetyScore(sid);
            sessions.add(SessionSummaryDto.builder()
                    .sessionId(sid.toString())
                    .driver(selected.getName())
                    .driverId(driverId.toString())
                    .startDate(extractDate(startTime))
                    .startTime(extractTime(startTime))
                    .duration(formatDurationHours(hours))
                    .safetyScore(score)
                    .alertsNumber(alerts)
                    .riskId(riskLevel(score))
                    .build());
        }

        List<Integer> performanceTrend = sessions.stream()
                .map(SessionSummaryDto::getSafetyScore)
                .toList();

        LocalDate trendFrom = range.to().minusDays(TREND_DAYS - 1L);
        String trendStartIso = startOfDayUtcIso(trendFrom);
        List<AlertTrendValueDto> alertTrendValues = buildDriverAlertTrendValues(
                driverId, trendStartIso, curEndExcl, trendFrom, range.to());
        List<PieDistributionDto> pieDistribution = buildDriverPieDistribution(driverId, curStartIso, curEndExcl);
        List<RiskDistributionDto> riskDistribution = buildDriverRiskDistribution(sessionsInPeriod);

        return Optional.of(DriverDetailResponseDto.builder()
                .filterId(DashboardFilterResolver.toFilterId(filter))
                .timeInterval(DashboardFilterResolver.formatTimeInterval(range.from(), range.to()))
                .overviewStats(List.of(
                        overviewStat("Total Sessions", sessionsCur, sessionsPrevLong, false),
                        overviewStat("Total High Risk Sessions", highRiskCur, highRiskPrev, false),
                        overviewStat("Total Alerts", alertsCur, alertsPrev, false),
                        overviewStat("Driving Hours", hoursCur, hoursPrev, false, true)))
                .selectedDriver(selected)
                .performanceTrend(performanceTrend)
                .alertTrendValues(alertTrendValues)
                .pieDistribution(pieDistribution)
                .riskDistribution(riskDistribution)
                .sessions(sessions)
                .build());
    }


    private DriverSummaryDto mapDriverRow(Object[] row, UUID companyId, String startIso, String endIsoExclusive) {
        UUID userId = (UUID) row[0];
        String name = (String) row[1];
        String email = (String) row[2];
        UserStatus status = (UserStatus) row[3];
        Instant insertedAt = (Instant) row[4];

        Map<UUID, long[]> agg = highLowByDriver(companyId, startIso, endIsoExclusive);
        long[] hl = agg.getOrDefault(userId, new long[]{0L, 0L});
        int score = (int) Math.round(individualScore(hl[0], hl[1]));
        if (!agg.containsKey(userId)) {
            score = 100;
        }

        List<String> lastStarts = repository.findLastSessionStartTime(userId, PageRequest.of(0, 1));

        return DriverSummaryDto.builder()
                .name(name)
                .id(userId.toString())
                .email(email)
                .riskId(riskLevel(score))
                .safetyScore(score)
                .totalSessions(repository.countSessionsForDriver(userId))
                .lastSession(lastStarts.isEmpty() ? "Never" : formatRelativeTime(lastStarts.get(0)))
                .joinedAt(JOINED_AT_FMT.format(insertedAt.atZone(ZoneOffset.UTC)))
                .status(formatUserStatus(status))
                .build();
    }


    private long countHighRiskDrivers(UUID companyId, String startIso, String endIsoExclusive) {
        Map<UUID, long[]> agg = highLowByDriver(companyId, startIso, endIsoExclusive);
        return agg.entrySet().stream()
                .filter(e -> riskLevel(individualScore(e.getValue()[0], e.getValue()[1])) == 2)
                .count();
    }

    private List<Integer> buildDriverPerformanceTrend(UUID driverId, int limit) {
        List<Object[]> sessions = repository.findSessionsForDriver(driverId);
        List<Integer> scores = new ArrayList<>();
        int count = Math.min(limit, sessions.size());
        for (int i = 0; i < count; i++) {
            UUID sid = (UUID) sessions.get(i)[0];
            scores.add(0, sessionSafetyScore(sid));
        }
        return scores;
    }

    private List<Integer> buildDriverAlertTrendSimple(UUID driverId, int limit) {
        List<Object[]> sessions = repository.findSessionsForDriver(driverId);
        List<Integer> alerts = new ArrayList<>();
        int count = Math.min(limit, sessions.size());
        for (int i = 0; i < count; i++) {
            int totalAlerts = sessions.get(i)[3] == null ? 0 : ((Number) sessions.get(i)[3]).intValue();
            alerts.add(0, totalAlerts);
        }
        return alerts;
    }

    private List<AlertTrendValueDto> buildDriverAlertTrendValues(
            UUID driverId, String trendStartIso, String endIsoExclusive,
            LocalDate trendFrom, LocalDate trendTo) {

        List<Object[]> rows = repository.countAlertsByDayAndTypeForDriver(driverId, trendStartIso, endIsoExclusive);
        Map<String, Map<Integer, Long>> byDayAndType = new HashMap<>();
        for (Object[] row : rows) {
            String dayKey = row[0].toString();
            DetectionType t = (DetectionType) row[1];
            Integer typeId = AlertTypeMapper.toTypeId(t);
            if (typeId == null || !AlertTypeMapper.isTrendType(typeId)) {
                continue;
            }
            long c = ((Number) row[2]).longValue();
            byDayAndType.computeIfAbsent(dayKey, k -> new HashMap<>()).merge(typeId, c, Long::sum);
        }

        long[] typeTotals = new long[3];
        List<AlertTrendValueDto> out = new ArrayList<>();
        for (int typeId = 0; typeId <= 2; typeId++) {
            List<Long> values = new ArrayList<>(TREND_DAYS);
            for (LocalDate d = trendFrom; !d.isAfter(trendTo); d = d.plusDays(1)) {
                String key = d.format(TREND_DAY_KEY);
                long count = byDayAndType.getOrDefault(key, Map.of()).getOrDefault(typeId, 0L);
                values.add(count);
                typeTotals[typeId] += count;
            }
            out.add(AlertTrendValueDto.builder().id(typeId).values(values).percent(0).build());
        }

        long grandTotal = typeTotals[0] + typeTotals[1] + typeTotals[2];
        for (int i = 0; i < out.size(); i++) {
            int percent = grandTotal == 0 ? 0 : (int) Math.round(typeTotals[i] * 100.0 / grandTotal);
            out.get(i).setPercent(percent);
        }
        return out;
    }

    private List<PieDistributionDto> buildDriverPieDistribution(UUID driverId, String startIso, String endIsoExclusive) {
        long[] perId = new long[3];
        long mappedTotal = 0;
        for (Object[] row : repository.countAlertsByTypeForDriver(driverId, startIso, endIsoExclusive)) {
            DetectionType t = (DetectionType) row[0];
            Integer id = AlertTypeMapper.toTypeId(t);
            long c = ((Number) row[1]).longValue();
            if (id == null || !AlertTypeMapper.isPieType(id)) {
                continue;
            }
            perId[id - 3] += c;
            mappedTotal += c;
        }

        List<PieDistributionDto> slices = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            int typeId = i + 3;
            int percent = mappedTotal == 0 ? 0 : (int) Math.round(perId[i] * 100.0 / mappedTotal);
            slices.add(PieDistributionDto.builder().id(typeId).percent(percent).build());
        }
        return slices;
    }

    private List<RiskDistributionDto> buildDriverRiskDistribution(List<Object[]> sessions) {
        long[] bucketCounts = new long[3];
        for (Object[] row : sessions) {
            UUID sid = (UUID) row[0];
            bucketCounts[riskLevel(sessionSafetyScore(sid))]++;
        }
        List<RiskDistributionDto> out = new ArrayList<>();
        for (int level = 0; level < 3; level++) {
            out.add(RiskDistributionDto.builder().id(level).value(bucketCounts[level]).build());
        }
        return out;
    }

    private Map<DetectionType, Long> alertTypeCountsForSession(UUID sessionId) {
        Map<DetectionType, Long> map = new HashMap<>();
        for (Object[] row : repository.countAlertsByTypeForSession(sessionId)) {
            map.put((DetectionType) row[0], ((Number) row[1]).longValue());
        }
        return map;
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

    private String formatRelativeTime(String isoTimestamp) {
        try {
            Instant instant = Instant.parse(isoTimestamp);
            Duration ago = Duration.between(instant, Instant.now());
            long minutes = ago.toMinutes();
            if (minutes < 1) {
                return "Just now";
            }
            if (minutes < 60) {
                return minutes + (minutes == 1 ? " minute ago" : " minutes ago");
            }
            long hours = ago.toHours();
            if (hours < 24) {
                return hours + (hours == 1 ? " hour ago" : " hours ago");
            }
            long days = ago.toDays();
            return days + (days == 1 ? " day ago" : " days ago");
        } catch (Exception ex) {
            return isoTimestamp;
        }
    }

    private String formatUserStatus(UserStatus status) {
        if (status == null) {
            return "Active";
        }
        return switch (status) {
            case ACTIVE -> "Active";
            case PENDING_VERIFICATION -> "Under Review";
        };
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

    private LocalDate[] previousInclusiveRange(LocalDate fromDate, LocalDate toDate) {
        long inclusiveDays = ChronoUnit.DAYS.between(fromDate, toDate) + 1;
        LocalDate previousEnd = fromDate.minusDays(1);
        LocalDate previousStart = previousEnd.minusDays(inclusiveDays - 1);
        return new LocalDate[]{previousStart, previousEnd};
    }

    private String startOfDayUtcIso(LocalDate day) {
        return day.atStartOfDay(ZoneOffset.UTC).toInstant().toString();
    }
}
