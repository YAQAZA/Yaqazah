package com.yaqazah.report.controller;

import com.yaqazah.report.service.ReportService;
import com.yaqazah.user.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/reports")
@Tag(name = "Reports", description = "Endpoints for generating system reports in various formats. Restricted to Company Admins.")
public class ReportController {

    @Autowired
    private ReportService reportService;

    @Operation(
            summary = "Generate CSV Report",
            description = "Triggers the generation of a system report in CSV format.",
            security = @SecurityRequirement(name = "bearerAuth") // Links to your OpenAPI security scheme
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "CSV report generated successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized (Missing or invalid JWT token)"),
            @ApiResponse(responseCode = "403", description = "Forbidden (Requires COMPANYADM role)"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error (Failed to write CSV stream)")
    })
    @GetMapping(value = "/csv", produces = "text/csv")
    @PreAuthorize("hasRole('COMPANYADMIN')")
    public void generateCSVReport(
            HttpServletResponse response,
            @Parameter(hidden = true) Authentication authentication // Prevents Swagger from asking for this as input
    ) throws IOException {

        // 2. Cast the principal to whatever class your JWT filter uses to store user data
        User user = (User) authentication.getPrincipal();

        // 3. Securely extract the company ID from the token payload
        UUID secureCompanyId = user.getCompanyId();

        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=\"driver_sessions_report.csv\"");

        // 4. Pass the secure ID to your service
        reportService.generateCSVReport(response.getWriter(), secureCompanyId);
    }
}