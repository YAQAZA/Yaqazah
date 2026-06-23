package com.yaqazah.user.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileResponseDto {
    private String email;
    private String fullName;
    private String gender;
    private String role;
}