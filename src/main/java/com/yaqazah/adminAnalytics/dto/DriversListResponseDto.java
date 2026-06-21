package com.yaqazah.adminAnalytics.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.yaqazah.dashboard.dto.OverviewStatDto;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@JsonPropertyOrder({
        "filterId",
        "performanceTrend",
        "alertTrendValues",
        "overviewStats",
        "drivers"
})
public class DriversListResponseDto {
    private String filterId;
    private List<OverviewStatDto> overviewStats;
//    private DriverSummaryDto selectedDriver;
    private List<Integer> performanceTrend;
    private List<Integer> alertTrendValues;
    private List<DriverSummaryDto> drivers;
}
