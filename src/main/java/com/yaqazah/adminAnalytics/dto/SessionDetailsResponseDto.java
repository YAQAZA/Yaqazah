package com.yaqazah.adminAnalytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionDetailsResponseDto {
    private SessionDetailDto session;
    private List<DetectionLogDto> logs;
}
