package com.yaqazah.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PieDistributionDto {
    @JsonProperty("id")
    private Integer id;
    private Integer percent;
}
