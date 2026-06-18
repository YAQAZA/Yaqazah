package com.yaqazah.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponseDto {
    private String filterId;
    private List<OverviewStatDto> overviewStats;
    private List<AlertTrendValueDto> alertTrendValues;
    private List<PieDistributionDto> pieDistribution;
    private List<RiskDistributionDto> riskDistribution;
    private List<RecentSessionDto> recentSessions;
    private List<TopPerformerDto> topPerformers;
}
