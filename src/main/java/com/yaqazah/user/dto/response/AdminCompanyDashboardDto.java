package com.yaqazah.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminCompanyDashboardDto {

    private CompanyInfoDto company;

    private UserProfileResponseDto user;

    private List<AdminListDto> admins;
}