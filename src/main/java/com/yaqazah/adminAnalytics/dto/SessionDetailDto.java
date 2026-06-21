package com.yaqazah.adminAnalytics.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SessionDetailDto {
    private String sessionId;
    private String driver;
    private String driverId;
    private String startDateTime;
    private String endDateTime;
    private String duration;
    private int safetyScore;
    private int alertsNumber;
    private int distractionCount;
    private int drowsyCount;
    private int sleepCount;
    private int riskId;
}
