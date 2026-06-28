package com.yaqazah.session.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "sessions")
public class Session {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID sessionId;
    private UUID userId;
    private String startDateTime;
    private String endDateTime;
    private double durationHours;
    private int totalAlerts;
    private String insertionTimestamp;
}