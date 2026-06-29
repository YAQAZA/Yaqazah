package com.yaqazah.user.controller;

import com.yaqazah.user.dto.request.LoginRequestDto;
import com.yaqazah.user.dto.response.AdminCompanyDashboardDto;
import com.yaqazah.user.dto.response.UserProfileResponseDto;
import com.yaqazah.user.model.User;
import com.yaqazah.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


@NullMarked
@RestController
@RequestMapping("/api/users/me")
@PreAuthorize("isAuthenticated()")
@Tag(
        name = "User Profile",
        description = "Endpoints for logged-in users"
)
@RequiredArgsConstructor
public class UserController {


    private final UserService userService;



    private String getCurrentUserEmail() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();


        if(authentication == null ||
                !authentication.isAuthenticated()) {

            throw new IllegalStateException(
                    "User is not authenticated"
            );
        }


        return authentication.getName();
    }

    @GetMapping
    @Operation(
            summary = "Get My Profile"
    )
    public ResponseEntity<UserProfileResponseDto> getMyProfile() {


        String email =
                getCurrentUserEmail();


        UserProfileResponseDto response =
                userService.getUserProfileDto(email);


        return ResponseEntity.ok(response);
    }

    @PatchMapping("/update-name")
    @Operation(
            summary = "Update Full Name"
    )
    public ResponseEntity<String> updateMyName(
            @RequestBody Map<String,String> body
    ) {


        String newName =
                body.get("fullName");


        userService.updateUserName(
                getCurrentUserEmail(),
                newName
        );


        return ResponseEntity.ok(
                "Name updated successfully"
        );
    }

    @DeleteMapping
    @Operation(
            summary = "Delete My Account"
    )
    public ResponseEntity<String> deleteMyAccount() {


        User user =
                userService.findByEmail(
                        getCurrentUserEmail()
                );


        userService.deleteAccount(
                user.getUserId()
        );


        return ResponseEntity.ok(
                "Account deleted successfully"
        );
    }

    @PostMapping("/restore")
    @Operation(
            summary = "Restore Account"
    )
    public ResponseEntity<String> restoreAccount(
            @RequestBody LoginRequestDto request
    ) {


        userService.restoreAccount(
                request.getEmail(),
                request.getPassword()
        );


        return ResponseEntity.ok(
                "Account restored successfully"
        );
    }

    @GetMapping("/admin-view")
    @PreAuthorize(
            "hasAnyRole('ADMIN','COMPANY_ADMIN')"
    )
    @Operation(
            summary = "Get Admin Company Dashboard",
            description =
                    "Returns profile, company info and admins"
    )
    public ResponseEntity<AdminCompanyDashboardDto> getAdminDashboard() {


        AdminCompanyDashboardDto response =
                userService.getAdminCompanyDashboard(
                        getCurrentUserEmail()
                );


        return ResponseEntity.ok(response);
    }

}