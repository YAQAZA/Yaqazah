package com.yaqazah.report.dto;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
public class DriverSessionReportDto {
    private UUID driverId;
    private String driverFullName;
    private UUID sessionId;

    // ALL dates are now properly Instants
    private Instant startTime;
    private Instant endTime;
    private Float durationHours;
    private Integer totalAlerts;

    // Detection Info
    private UUID eventId;
    private Instant eventTimestamp;
    private String detectionType;
    private String severity;
    private Float valueDetected;

    // The explicit constructor to keep Hibernate happy
    public DriverSessionReportDto(
            UUID driverId,
            String driverFullName,
            UUID sessionId,
            Instant startTime,
            Instant endTime,
            float durationHours,
            int totalAlerts,
            UUID eventId,
            Instant eventTimestamp,
            String detectionType,
            String severity,
            float valueDetected) {

        this.driverId = driverId;
        this.driverFullName = driverFullName;
        this.sessionId = sessionId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.durationHours = durationHours;
        this.totalAlerts = totalAlerts;
        this.eventId = eventId;
        this.eventTimestamp = eventTimestamp;
        this.detectionType = detectionType;
        this.severity = severity;
        this.valueDetected = valueDetected;
    }
}