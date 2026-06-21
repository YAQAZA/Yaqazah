package com.yaqazah.adminAnalytics.dto;

import com.yaqazah.dashboard.dto.OverviewStatDto;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SessionsListResponseDto {
    private String filterId;
    private String timeInterval;
    private List<OverviewStatDto> overviewStats;
    private List<SessionSummaryDto> sessions;
}
