package com.yaqazah.session.model;

import com.yaqazah.user.model.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "session")
public class Session {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID sessionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", nullable = false)
    private User user;
    @Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private Instant startTime;
    @Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private Instant  endTime;
    private float durationHours;
    private int totalAlerts;

    @Enumerated(EnumType.STRING)
    private SessionStatus status;

}