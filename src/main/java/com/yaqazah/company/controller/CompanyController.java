package com.yaqazah.company.controller;

import com.yaqazah.company.model.Company;
import com.yaqazah.company.service.CompanyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@NullMarked
@RequestMapping("/api/companies")
@Tag(name = "Company Management", description = "Endpoints for managing fleets and companies.")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    @Operation(summary = "Get Company by ID", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/{companyId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_ADMIN')") // Both roles can access
    public ResponseEntity<?> getCompanyById(@PathVariable UUID companyId, Principal principal) {
        try {
            return companyService.getCompanyById(companyId, principal.getName())
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    @Operation(summary = "Delete Company", security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/{companyId}")
    @PreAuthorize("hasRole('ADMIN')") // ONLY Admin can delete
    public ResponseEntity<?> deleteCompany(@PathVariable UUID companyId, Principal principal) {
        try {
            companyService.deleteCompany(companyId, principal.getName());
            return ResponseEntity.ok("Company deleted successfully.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }
}