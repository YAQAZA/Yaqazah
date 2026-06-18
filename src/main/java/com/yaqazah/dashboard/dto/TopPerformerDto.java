package com.yaqazah.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopPerformerDto {
    private String name;
    private Long sessions;
    private Double score;
    private Integer rank;
}
