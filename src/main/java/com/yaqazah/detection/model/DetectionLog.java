package com.yaqazah.detection.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Setter @Getter @NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "detection_event")
public class DetectionLog {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID eventId;

    private UUID sessionId;
    private String timestamp;

    @Enumerated(EnumType.STRING)
    private DetectionType type;

    private String severity;
    private float valueDetected;

    @Lob
    @Column(name = "snapshot_image")
    private byte[] snapshotImage;
}