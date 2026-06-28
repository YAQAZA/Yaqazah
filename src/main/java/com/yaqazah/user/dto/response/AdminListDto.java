package com.yaqazah.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminListDto {
    private String name;
    private String email;
    private Instant insertedAt;
    private String role;
}