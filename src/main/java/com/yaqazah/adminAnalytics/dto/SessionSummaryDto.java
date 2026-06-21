package com.yaqazah.adminAnalytics.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SessionSummaryDto {
    private String sessionId;
    private String driver;
    private String driverId;
    private String startDateTime;
    private String startDate;
    private String startTime;
    private String duration;
    private int safetyScore;
    private int alertsNumber;
    private int riskId;
}
