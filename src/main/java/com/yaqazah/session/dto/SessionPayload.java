package com.yaqazah.session.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionPayload {
    private UUID userId;
    private String startDateTime;
    private String endDateTime;
    private double duration; // duration in hours sent by frontend
}
