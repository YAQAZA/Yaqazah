package com.yaqazah.user.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponseDto {
    private String token;
    private String refreshToken;
    private AuthResponseDto user;
}