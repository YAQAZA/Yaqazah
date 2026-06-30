package com.yaqazah.report.controller;

import com.yaqazah.report.model.Report;
import com.yaqazah.report.repository.ReportRepository;
import com.yaqazah.report.service.ReportService;
import com.yaqazah.user.model.Role;
import com.yaqazah.user.model.User;
import com.yaqazah.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/reports")
@Tag(name = "Reports", description = "Endpoints for generating system reports in various formats.")
public class ReportController {

    private final ReportService reportService;
    private final UserRepository userRepository;
    private final ReportRepository reportRepository;

    public ReportController(
            ReportService reportService,
            UserRepository userRepository,
            ReportRepository reportRepository) {
        this.reportService = reportService;
        this.userRepository = userRepository;
        this.reportRepository = reportRepository;
    }

    @Operation(
            summary = "Generate Combined CSV Report",
            description = "Triggers the generation of a system report in CSV format combining driver and session details.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "CSV report generated successfully"),
            @ApiResponse(responseCode = "400", description = "Bad Request (e.g. missing companyId for admin)"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    @GetMapping(value = "/csv", produces = "text/csv")
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN', 'ADMIN')")
    public void generateCSVReport(
            @RequestParam(value = "companyId", required = false) UUID companyId,
            HttpServletResponse response,
            @Parameter(hidden = true) Authentication authentication
    ) throws IOException {
        User user = getAuthenticatedUser(authentication);
        UUID targetCompanyId = resolveTargetCompanyId(user, companyId, response);
        if (targetCompanyId == null) {
            return;
        }

        try {
            response.setContentType("text/csv");
            response.setHeader("Content-Disposition", "attachment; filename=\"driver_sessions_report.csv\"");
            String csvContent = reportService.generateCSVReport(targetCompanyId);
            response.getWriter().write(csvContent);
            saveReportLog("CSV", "Combined", targetCompanyId, user.getEmail());
        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to generate CSV: " + e.getMessage());
        }
    }

    @Operation(
            summary = "Export Dashboard Data to CSV",
            description = "Generates a CSV report summarizing the dashboard overview stats, risk distribution, and alert distribution.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "CSV dashboard data generated successfully"),
            @ApiResponse(responseCode = "400", description = "Bad Request (e.g. missing companyId for admin)"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    @GetMapping(value = "/dashboard/csv", produces = "text/csv")
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN', 'ADMIN')")
    public void exportDashboardCSV(
            @RequestParam("filter") String filter,
            @RequestParam(value = "from", required = false) String from,
            @RequestParam(value = "to", required = false) String to,
            @RequestParam(value = "companyId", required = false) UUID companyId,
            HttpServletResponse response,
            @Parameter(hidden = true) Authentication authentication
    ) throws IOException {
        User user = getAuthenticatedUser(authentication);
        UUID targetCompanyId = resolveTargetCompanyId(user, companyId, response);
        if (targetCompanyId == null) {
            return;
        }

        try {
            response.setContentType("text/csv");
            response.setHeader("Content-Disposition", "attachment; filename=\"dashboard_report.csv\"");
            String csvContent = reportService.generateDashboardCSV(targetCompanyId, filter, from, to);
            response.getWriter().write(csvContent);
            saveReportLog("CSV", "Dashboard", targetCompanyId, user.getEmail());
        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to generate CSV: " + e.getMessage());
        }
    }

    @Operation(
            summary = "Export Sessions Data to CSV",
            description = "Generates a CSV report listing all session summaries.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "CSV sessions data generated successfully"),
            @ApiResponse(responseCode = "400", description = "Bad Request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    @GetMapping(value = "/sessions/csv", produces = "text/csv")
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN', 'ADMIN')")
    public void exportSessionsCSV(
            @RequestParam("filter") String filter,
            @RequestParam(value = "from", required = false) String from,
            @RequestParam(value = "to", required = false) String to,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "risk", required = false) String risk,
            @RequestParam(value = "sort", required = false) String sort,
            @RequestParam(value = "companyId", required = false) UUID companyId,
            HttpServletResponse response,
            @Parameter(hidden = true) Authentication authentication
    ) throws IOException {
        User user = getAuthenticatedUser(authentication);
        UUID targetCompanyId = resolveTargetCompanyId(user, companyId, response);
        if (targetCompanyId == null) {
            return;
        }

        try {
            response.setContentType("text/csv");
            response.setHeader("Content-Disposition", "attachment; filename=\"sessions_report.csv\"");
            String csvContent = reportService.generateSessionsCSV(targetCompanyId, filter, from, to, search, risk, sort);
            response.getWriter().write(csvContent);
            saveReportLog("CSV", "Sessions", targetCompanyId, user.getEmail());
        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to generate CSV: " + e.getMessage());
        }
    }

    @Operation(
            summary = "Export Drivers Data to CSV",
            description = "Generates a CSV report listing all drivers and their overview analytics.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "CSV drivers data generated successfully"),
            @ApiResponse(responseCode = "400", description = "Bad Request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    @GetMapping(value = "/drivers/csv", produces = "text/csv")
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN', 'ADMIN')")
    public void exportDriversCSV(
            @RequestParam("filter") String filter,
            @RequestParam(value = "from", required = false) String from,
            @RequestParam(value = "to", required = false) String to,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "sort", required = false) String sort,
            @RequestParam(value = "companyId", required = false) UUID companyId,
            HttpServletResponse response,
            @Parameter(hidden = true) Authentication authentication
    ) throws IOException {
        User user = getAuthenticatedUser(authentication);
        UUID targetCompanyId = resolveTargetCompanyId(user, companyId, response);
        if (targetCompanyId == null) {
            return;
        }

        try {
            response.setContentType("text/csv");
            response.setHeader("Content-Disposition", "attachment; filename=\"drivers_report.csv\"");
            String csvContent = reportService.generateDriversCSV(targetCompanyId, filter, from, to, search, sort);
            response.getWriter().write(csvContent);
            saveReportLog("CSV", "Drivers", targetCompanyId, user.getEmail());
        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to generate CSV: " + e.getMessage());
        }
    }

    private User getAuthenticatedUser(Authentication authentication) {
        UserDetails principal = (UserDetails) authentication.getPrincipal();
        return userRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new IllegalStateException("User not found"));
    }

    private UUID resolveTargetCompanyId(User user, UUID requestedCompanyId, HttpServletResponse response) throws IOException {
        if (user.getRole() == Role.COMPANY_ADMIN) {
            if (user.getCompany() == null) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Company Admin is not assigned to a company.");
                return null;
            }
            return user.getCompany().getCompanyId();
        } else if (user.getRole() == Role.ADMIN) {
            if (requestedCompanyId != null) {
                return requestedCompanyId;
            }
            if (user.getCompany() != null) {
                return user.getCompany().getCompanyId();
            }
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "companyId query parameter is required for System Administrators.");
            return null;
        } else {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Unauthorized role.");
            return null;
        }
    }

    private void saveReportLog(String format, String category, UUID companyId, String email) {
        try {
            Report report = new Report();
            report.setFormat(format);
            report.setFilePath("streamed");
            report.setContent(String.format("%s report generated for company %s by user %s", category, companyId, email));
            reportRepository.save(report);
        } catch (Exception e) {
            System.err.println("Failed to save report audit log: " + e.getMessage());
        }
    }
}