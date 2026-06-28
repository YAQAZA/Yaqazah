package com.yaqazah.session.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogPayload {
    private String title;
    private String subtitle;
    private String timestamp;
    private int riskId;
    private int alertId;
    private String snapshotUrl;
    private String photoBase64;
}
