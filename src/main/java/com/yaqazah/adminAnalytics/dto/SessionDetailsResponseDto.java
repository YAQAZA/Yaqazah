package com.yaqazah.adminAnalytics.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SessionDetailsResponseDto {
    private SessionDetailDto session;
    private List<DetectionLogDto> logs;
}
