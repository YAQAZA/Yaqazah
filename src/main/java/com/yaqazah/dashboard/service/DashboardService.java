package com.yaqazah.dashboard.service;

import com.yaqazah.dashboard.dto.AlertTrendValueDto;
import com.yaqazah.dashboard.dto.DashboardResponseDto;
import com.yaqazah.dashboard.dto.OverviewStatDto;
import com.yaqazah.dashboard.dto.PieDistributionDto;
import com.yaqazah.dashboard.dto.RecentSessionDto;
import com.yaqazah.dashboard.dto.RiskDistributionDto;
import com.yaqazah.dashboard.dto.TopPerformerDto;
import com.yaqazah.dashboard.repository.DashboardRepository;
import com.yaqazah.dashboard.util.AlertTypeMapper;
import com.yaqazah.dashboard.util.DashboardFilterResolver;
import com.yaqazah.dashboard.util.DashboardFilterResolver.DateRange;
import com.yaqazah.detection.model.DetectionType;
import com.yaqazah.user.model.Role;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private static final DateTimeFormatter TREND_DAY_KEY = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final int TREND_DAYS = 7;
    private static final int RECENT_SESSION_LIMIT = 3;
    private static final int TOP_PERFORMER_LIMIT = 3;

    private final DashboardRepository dashboardRepository;

    public DashboardService(DashboardRepository dashboardRepository) {
        this.dashboardRepository = dashboardRepository;
    }

    @Transactional(readOnly = true)
    public DashboardResponseDto buildDashboard(UUID companyId, String filter, String fromIso, String toIso) {
        DateRange range = DashboardFilterResolver.resolve(filter, fromIso, toIso);
        LocalDate fromDate = range.from();
        LocalDate toDate = range.to();

        LocalDate[] previous = previousInclusiveRange(fromDate, toDate);
        LocalDate prevFrom = previous[0];
        LocalDate prevTo = previous[1];

        String curStartIso = startOfDayUtcIso(fromDate);
        String curEndExcl = startOfDayUtcIso(toDate.plusDays(1));
        String prevStartIso = startOfDayUtcIso(prevFrom);
        String prevEndExcl = startOfDayUtcIso(prevTo.plusDays(1));

        long sessionsCur = dashboardRepository.countSessions(companyId, Role.FLEET_DRIVER, curStartIso, curEndExcl);
        long sessionsPrev = dashboardRepository.countSessions(companyId, Role.FLEET_DRIVER, prevStartIso, prevEndExcl);

        long activeCur = dashboardRepository.countActiveDrivers(companyId, Role.FLEET_DRIVER, curStartIso, curEndExcl);
        long activePrev = dashboardRepository.countActiveDrivers(companyId, Role.FLEET_DRIVER, prevStartIso, prevEndExcl);

        long alertsCur = dashboardRepository.countTotalAlerts(companyId, curStartIso, curEndExcl);
        long alertsPrev = dashboardRepository.countTotalAlerts(companyId, prevStartIso, prevEndExcl);

        Double avgScoreCur = companyAverageSafetyScore(companyId, curStartIso, curEndExcl);
        Double avgScorePrev = companyAverageSafetyScore(companyId, prevStartIso, prevEndExcl);

        List<OverviewStatDto> overviewStats = List.of(
                overviewStat("Total Sessions", sessionsCur, sessionsPrev, false),
                overviewStat("Active Drivers", activeCur, activePrev, false),
                overviewStat("Average Safety Score", avgScoreCur, avgScorePrev, true),
                overviewStat("Total Alerts", alertsCur, alertsPrev, false)
        );

        LocalDate trendFrom = toDate.minusDays(TREND_DAYS - 1L);
        String trendStartIso = startOfDayUtcIso(trendFrom);

        List<AlertTrendValueDto> alertTrendValues = buildAlertTrendValues(
                companyId, trendStartIso, curEndExcl, trendFrom, toDate);

        List<PieDistributionDto> pieDistribution = buildPieDistribution(companyId, curStartIso, curEndExcl);
        List<RiskDistributionDto> riskDistribution = buildRiskDistribution(companyId, curStartIso, curEndExcl);
        List<RecentSessionDto> recentSessions = buildRecentSessions(companyId);
        List<TopPerformerDto> topPerformers = buildTopPerformers(companyId, curStartIso, curEndExcl);

        return DashboardResponseDto.builder()
                .filterId(DashboardFilterResolver.toFilterId(filter))
                .overviewStats(overviewStats)
                .alertTrendValues(alertTrendValues)
                .pieDistribution(pieDistribution)
                .riskDistribution(riskDistribution)
                .recentSessions(recentSessions)
                .topPerformers(topPerformers)
                .build();
    }

    private OverviewStatDto overviewStat(String label, Double current, Double previous, boolean asPercent) {
        return OverviewStatDto.builder()
                .label(label)
                .value(formatOverviewValue(current, asPercent))
                .delta(formatDeltaPercent(deltaPercent(current, previous)))
                .build();
    }

    private OverviewStatDto overviewStat(String label, long current, long previous, boolean asPercent) {
        return overviewStat(label, (double) current, (double) previous, asPercent);
    }

    private String formatOverviewValue(Double value, boolean asPercent) {
        if (value == null) {
            return asPercent ? "0%" : "0";
        }
        if (asPercent) {
            return Math.round(value) + "%";
        }
        return String.valueOf(Math.round(value));
    }

    private Double deltaPercent(Double current, Double previous) {
        if (previous == null || previous == 0.0) {
            return (current == null || current == 0.0) ? 0.0 : 100.0;
        }
        if (current == null) {
            return -100.0;
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

    private LocalDate[] previousInclusiveRange(LocalDate fromDate, LocalDate toDate) {
        long inclusiveDays = ChronoUnit.DAYS.between(fromDate, toDate) + 1;
        LocalDate previousEnd = fromDate.minusDays(1);
        LocalDate previousStart = previousEnd.minusDays(inclusiveDays - 1);
        return new LocalDate[]{previousStart, previousEnd};
    }

    private String startOfDayUtcIso(LocalDate day) {
        return day.atStartOfDay(ZoneOffset.UTC).toInstant().toString();
    }

    private Double companyAverageSafetyScore(UUID companyId, String startIso, String endIsoExclusive) {
        Map<UUID, long[]> highLow = highLowByDriver(companyId, startIso, endIsoExclusive);
        long active = dashboardRepository.countActiveDrivers(companyId, Role.FLEET_DRIVER, startIso, endIsoExclusive);
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
        for (Object[] row : dashboardRepository.sumHighLowByDriver(companyId, startIso, endIsoExclusive)) {
            UUID userId = (UUID) row[0];
            long high = row[1] == null ? 0L : ((Number) row[1]).longValue();
            long low = row[2] == null ? 0L : ((Number) row[2]).longValue();
            map.put(userId, new long[]{high, low});
        }
        return map;
    }

    private double individualScore(long highCount, long lowCount) {
        double raw = 100.0 - (5.0 * highCount + 2.0 * lowCount);
        return Math.max(0.0, raw);
    }

    private List<AlertTrendValueDto> buildAlertTrendValues(
            UUID companyId,
            String trendStartIso,
            String endIsoExclusive,
            LocalDate trendFrom,
            LocalDate trendTo) {

        List<Object[]> rows = dashboardRepository.countAlertsByDayAndType(
                companyId, trendStartIso, endIsoExclusive);

        Map<String, Map<Integer, Long>> byDayAndType = new HashMap<>();
        for (Object[] row : rows) {
            String dayKey = (String) row[0];
            DetectionType t = (DetectionType) row[1];
            Integer typeId = AlertTypeMapper.toTypeId(t);
            if (typeId == null || !AlertTypeMapper.isTrendType(typeId)) {
                continue;
            }
            long c = ((Number) row[2]).longValue();
            byDayAndType
                    .computeIfAbsent(dayKey, k -> new HashMap<>())
                    .merge(typeId, c, Long::sum);
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
            while (values.size() < TREND_DAYS) {
                values.add(0L);
            }
            out.add(AlertTrendValueDto.builder()
                    .id(typeId)
                    .values(values)
                    .percent(0)
                    .build());
        }

        long grandTotal = typeTotals[0] + typeTotals[1] + typeTotals[2];
        for (int i = 0; i < out.size(); i++) {
            int percent = grandTotal == 0 ? 0 : (int) Math.round(typeTotals[i] * 100.0 / grandTotal);
            out.get(i).setPercent(percent);
        }
        return out;
    }

    private List<PieDistributionDto> buildPieDistribution(UUID companyId, String startIso, String endIsoExclusive) {
        long[] perId = new long[3];
        long mappedTotal = 0;
        for (Object[] row : dashboardRepository.countAlertsByType(companyId, startIso, endIsoExclusive)) {
            DetectionType t = (DetectionType) row[0];
            Integer id = AlertTypeMapper.toTypeId(t);
            long c = ((Number) row[1]).longValue();
            if (id == null || !AlertTypeMapper.isPieType(id)) {
                continue;
            }
            int index = id - 3;
            perId[index] += c;
            mappedTotal += c;
        }

        List<PieDistributionDto> slices = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            int typeId = i + 3;
            int percent = mappedTotal == 0 ? 0 : (int) Math.round(perId[i] * 100.0 / mappedTotal);
            slices.add(PieDistributionDto.builder()
                    .id(typeId)
                    .percent(percent)
                    .build());
        }
        return slices;
    }

    private List<RiskDistributionDto> buildRiskDistribution(UUID companyId, String startIso, String endIsoExclusive) {
        Map<UUID, long[]> agg = highLowByDriver(companyId, startIso, endIsoExclusive);
        long active = dashboardRepository.countActiveDrivers(companyId, Role.FLEET_DRIVER, startIso, endIsoExclusive);

        long[] bucketCounts = new long[3];

        for (Map.Entry<UUID, long[]> e : agg.entrySet()) {
            double score = individualScore(e.getValue()[0], e.getValue()[1]);
            bucketCounts[riskLevel(score)]++;
        }
        long withoutAlerts = active - agg.size();
        bucketCounts[0] += withoutAlerts;

        List<RiskDistributionDto> out = new ArrayList<>();
        for (int level = 0; level < 3; level++) {
            out.add(RiskDistributionDto.builder()
                    .id(level)
                    .value(bucketCounts[level])
                    .build());
        }
        return out;
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

    private List<RecentSessionDto> buildRecentSessions(UUID companyId) {
        List<Object[]> rows = dashboardRepository.findRecentSessionsForCompany(
                companyId, Role.FLEET_DRIVER, PageRequest.of(0, RECENT_SESSION_LIMIT));
        List<RecentSessionDto> list = new ArrayList<>();
        for (Object[] row : rows) {
            UUID sessionId = (UUID) row[0];
            String name = (String) row[1];
            float hours = row[2] == null ? 0f : ((Number) row[2]).floatValue();
            int alerts = row[3] == null ? 0 : ((Number) row[3]).intValue();
            list.add(RecentSessionDto.builder()
                    .driver(name)
                    .riskId(sessionRiskId(sessionId))
                    .duration(formatDuration(hours))
                    .alerts(alerts)
                    .build());
        }
        return list;
    }

    private int sessionRiskId(UUID sessionId) {
        Object[] row = dashboardRepository.sumHighLowForSession(sessionId);
        if (row == null) {
            return 0;
        }
        long high = row[0] == null ? 0L : ((Number) row[0]).longValue();
        long low = row[1] == null ? 0L : ((Number) row[1]).longValue();
        return riskLevel(individualScore(high, low));
    }

    private String formatDuration(float durationHours) {
        int totalMinutes = Math.round(durationHours * 60);
        if (totalMinutes <= 0) {
            return "0 mins";
        }
        return totalMinutes + " mins";
    }

    private List<TopPerformerDto> buildTopPerformers(UUID companyId, String startIso, String endIsoExclusive) {
        List<UUID> activeIds = dashboardRepository.findActiveDriverIdsInPeriod(
                companyId, Role.FLEET_DRIVER, startIso, endIsoExclusive);
        if (activeIds.isEmpty()) {
            return List.of();
        }

        Map<UUID, long[]> agg = highLowByDriver(companyId, startIso, endIsoExclusive);
        Map<UUID, Long> sessionsByDriver = sessionsByDriver(companyId, startIso, endIsoExclusive);

        Map<UUID, Double> scoreByUser = new HashMap<>();
        for (UUID id : activeIds) {
            long[] hl = agg.getOrDefault(id, new long[]{0L, 0L});
            scoreByUser.put(id, individualScore(hl[0], hl[1]));
        }

        List<UUID> topIds = scoreByUser.entrySet().stream()
                .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed())
                .limit(TOP_PERFORMER_LIMIT)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        Map<UUID, String> names = loadDriverNames(topIds);

        List<TopPerformerDto> out = new ArrayList<>();
        int rank = 1;
        for (UUID id : topIds) {
            out.add(TopPerformerDto.builder()
                    .name(names.getOrDefault(id, ""))
                    .sessions(sessionsByDriver.getOrDefault(id, 0L))
                    .score(scoreByUser.get(id))
                    .rank(rank++)
                    .build());
        }
        return out;
    }

    private Map<UUID, Long> sessionsByDriver(UUID companyId, String startIso, String endIsoExclusive) {
        Map<UUID, Long> map = new HashMap<>();
        for (Object[] row : dashboardRepository.countSessionsByDriver(
                companyId, Role.FLEET_DRIVER, startIso, endIsoExclusive)) {
            map.put((UUID) row[0], ((Number) row[1]).longValue());
        }
        return map;
    }

    private Map<UUID, String> loadDriverNames(List<UUID> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, String> map = new HashMap<>();
        for (Object[] row : dashboardRepository.findDriverNamesByIds(userIds)) {
            map.put((UUID) row[0], row[1] != null ? (String) row[1] : "");
        }
        return map;
    }
}
