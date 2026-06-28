package com.yaqazah.adminAnalytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DetectionLogDto {
    private String eventId;
    private String timestamp;
    private int typeId;
    private String severity;
    private float valueDetected;
    private String snapshotUrl;
}
