package com.yaqazah.detection.model;

import com.yaqazah.user.model.User;
import com.yaqazah.session.model.Session;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String timestamp;

    @Enumerated(EnumType.STRING)
    private DetectionType type;

    private String severity;
    private float valueDetected;

    @Column(columnDefinition = "TEXT")
    private String snapshotUrl;
}