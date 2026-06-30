package com.yaqazah.adminAnalytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionSummaryDto {
    private String sessionId;
    private String driver;
    private String driverId;
    private String startDateTime;
    private String endDateTime;
    private String duration;
    private int safetyScore;
    private int alertsNumber;
    private int riskId;
    private int distractionCount; //add
    private int drowsyCount; //add
    private int sleepCount; //add
}
