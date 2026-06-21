package com.yaqazah.adminAnalytics.dto;

import com.yaqazah.dashboard.dto.AlertTrendValueDto;
import com.yaqazah.dashboard.dto.OverviewStatDto;
import com.yaqazah.dashboard.dto.PieDistributionDto;
import com.yaqazah.dashboard.dto.RiskDistributionDto;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
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
}
