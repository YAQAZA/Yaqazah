package com.yaqazah.company.controller;

import com.yaqazah.company.model.Company;
import com.yaqazah.company.service.CompanyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/companies")
@Tag(name = "Company Management", description = "Endpoints for managing fleets and companies. Restricted to System Admins.")
public class CompanyController {

    @Autowired
    private CompanyService companyService;

    @Operation(summary = "Create a new Company", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/add")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createCompany(@RequestBody Company company) {
        try {
            // The service handles setting the UUID and the createdAt timestamp
            Company savedCompany = companyService.createCompany(company);
            return ResponseEntity.ok(savedCompany);
        } catch (IllegalArgumentException e) {
            // Catches the duplicate name error from the service and returns a 400 Bad Request
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "Get Company by ID", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/{companyId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Company> getCompanyById(@PathVariable UUID companyId) {
        return companyService.getCompanyById(companyId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Get all Companies", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Company>> getAllCompanies() {
        List<Company> companies = companyService.getAllCompanies();
        return ResponseEntity.ok(companies);
    }

    @Operation(summary = "Delete Company", security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/{companyId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteCompany(@PathVariable UUID companyId) {
        try {
            companyService.deleteCompany(companyId);
            return ResponseEntity.ok("Company deleted successfully.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}