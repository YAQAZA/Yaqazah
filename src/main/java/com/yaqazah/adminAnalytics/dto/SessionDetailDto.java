package com.yaqazah.adminAnalytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
