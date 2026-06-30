package com.yaqazah.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

@JsonPropertyOrder({
        "filterId",
        "timeInterval",
        "overviewStats",
        "trendLabels",
        "alertTrendValues",
        "pieDistribution",
        "riskDistribution",
        "recentSessions",
        "topPerformers"
})
public class DashboardResponseDto {
    private String filterId;
    private String timeInterval;
    private List<OverviewStatDto> overviewStats;
    private List<AlertTrendValueDto> alertTrendValues;
    private List<PieDistributionDto> pieDistribution;
    private List<RiskDistributionDto> riskDistribution;
    private List<RecentSessionDto> recentSessions;
    private List<TopPerformerDto> topPerformers;
    private List<String> trendLabels; // <-- ADD THIS
}
