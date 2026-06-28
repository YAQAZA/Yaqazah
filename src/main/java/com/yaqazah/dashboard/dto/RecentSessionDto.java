package com.yaqazah.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecentSessionDto {
    private String driver;
    private Integer riskId;
    private String duration;
    private Integer alerts;
}
