package com.yaqazah.report.dto;

import java.util.UUID;

public record DriverSessionReportDto(
        UUID driverId,
        String driverFullName,
        UUID sessionId,
        String startTime,
        String endTime,
        Float durationHours,
        Integer totalAlerts,

        // Detection Info (Will be null if the session had no alerts)
        UUID eventId,
        String eventTimestamp,
        String detectionType,
        String severity,
        Float valueDetected
) {}