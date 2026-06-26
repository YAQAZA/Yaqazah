package com.yaqazah.detection.dto;

import com.yaqazah.detection.model.DetectionType;
import lombok.*;
import java.util.UUID;

@Data // This automatically adds Getters, Setters, and RequiredArgsConstructor
@NoArgsConstructor
@AllArgsConstructor
public class DetectionRequest {
    private UUID sessionId;
    private java.time.Instant timestamp;
    private DetectionType type;

    // ADD THIS FIELD
    private String severity;

    private float valueDetected;
    private String photoBase64;
}