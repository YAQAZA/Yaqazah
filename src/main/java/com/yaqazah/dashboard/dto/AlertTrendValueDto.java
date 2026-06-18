package com.yaqazah.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertTrendValueDto {
    @JsonProperty("id")
    private Integer id;
    private Integer percent;
    private List<Long> values;
}
