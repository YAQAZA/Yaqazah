package com.yaqazah.adminAnalytics.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.yaqazah.dashboard.dto.OverviewStatDto;
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
        "performanceTrend",
        "alertTrendValues",
        "overviewStats",
        "drivers"
})
public class DriversListResponseDto {
    private String filterId;
    private String timeInterval;
    private List<OverviewStatDto> overviewStats;
    private List<DriverSummaryDto> drivers;
}
