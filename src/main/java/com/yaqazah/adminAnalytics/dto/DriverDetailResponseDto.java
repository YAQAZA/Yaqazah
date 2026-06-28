package com.yaqazah.adminAnalytics.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.yaqazah.dashboard.dto.AlertTrendValueDto;
import com.yaqazah.dashboard.dto.OverviewStatDto;
import com.yaqazah.dashboard.dto.PieDistributionDto;
import com.yaqazah.dashboard.dto.RiskDistributionDto;
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
        "performanceTrend",
        "alertTrendValues",
        "pieDistribution",
        "riskDistribution",
        "selectedDriver",
        "sessions"
})
public class DriverDetailResponseDto {
    private String filterId;
    private String timeInterval;
    private List<OverviewStatDto> overviewStats;
    private DriverSummaryDto selectedDriver;
    private List<Integer> performanceTrend;
    private List<AlertTrendValueDto> alertTrendValues;
    private List<PieDistributionDto> pieDistribution;
    private List<RiskDistributionDto> riskDistribution;
    private List<SessionSummaryDto> sessions;
    private List<String> trendLabels; // <-- ADD THIS

}
