package com.yaqazah.user.controller;

import com.yaqazah.user.dto.request.FleetDriverDto;
import com.yaqazah.user.dto.request.UpdateFleetDriverDto;
import com.yaqazah.user.service.CompanyAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@NullMarked
@RestController
@RequestMapping("/api/company/drivers")
@PreAuthorize("hasRole('COMPANY_ADMIN')")
@Tag(name = "Company Driver Management", description = "Endpoints for Company Admins to manage their fleet drivers")
@RequiredArgsConstructor
public class CompanyAdminController {

    private final CompanyAdminService companyAdminService;

    private String getCurrentAdminEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }

    @PostMapping("/add")
    @Operation(summary = "Add a new Fleet Driver", description = "Creates a new driver account linked to the logged-in admin's company.")
    // FIX: Changed User to FleetDriverDto and added @Valid
    public ResponseEntity<String> addFleetDriver(@Valid @RequestBody FleetDriverDto newDriver) {
        try {
            companyAdminService.addFleetDriver(newDriver, getCurrentAdminEmail());
            return ResponseEntity.ok("Fleet Driver added successfully to your company!");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    @PutMapping("/edit/{driverId}")
    @Operation(summary = "Edit a Fleet Driver", description = "Updates details of an existing driver. Restricted to drivers within the admin's company.")
    // FIX: Changed User to UpdateFleetDriverDto and added @Valid
    public ResponseEntity<String> editFleetDriver(
            @PathVariable UUID driverId,
            @Valid @RequestBody UpdateFleetDriverDto updatedDriverData) {
        try {
            companyAdminService.updateFleetDriver(driverId, updatedDriverData, getCurrentAdminEmail());
            return ResponseEntity.ok("Fleet Driver updated successfully!");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }
}