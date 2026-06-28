package com.yaqazah.userAnalytics.model;

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
        "trendLabels",
        "performanceTrend",
        "alertTrendValues",
        "overviewStats",
        "pieDistribution",
        "riskDistribution"
})
public class UserAnalyticsResponseDto {
    private String filterId;
    private String timeInterval;
    private List<OverviewStatDto> overviewStats;
    private List<Integer> performanceTrend;
    private List<AlertTrendValueDto> alertTrendValues;
    private List<PieDistributionDto> pieDistribution;
    private List<RiskDistributionDto> riskDistribution;
    private List<String> trendLabels; //<-- I added this
}
