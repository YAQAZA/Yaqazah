package com.yaqazah.detection.dto;

import com.yaqazah.detection.model.DetectionType;
import lombok.Data;
import java.util.UUID;

@Data
public class DetectionRequest {
    private UUID sessionId;
    private String timestamp;
    private DetectionType type;
    private float valueDetected;
    private String photoBase64;
}