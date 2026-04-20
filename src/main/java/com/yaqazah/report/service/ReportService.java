package com.yaqazah.report.service;

import com.yaqazah.report.dto.DriverSessionReportDto;
import com.yaqazah.user.repository.UserRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.UUID;

@Service
public class ReportService {

    @Autowired
    private UserRepository userRepository;

    public void generateCSVReport(Writer writer, UUID companyId) {

        List<DriverSessionReportDto> data = userRepository.findCombinedDriverDataByCompany(companyId);

        // Update headers to include everything
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader("Driver ID", "Driver Name", "Session ID", "Start Time", "End Time",
                        "Duration (Hrs)", "Total Alerts", "Event ID", "Event Time", "Alert Type",
                        "Severity", "Value Detected")
                .build();

        try (CSVPrinter csvPrinter = new CSVPrinter(writer, format)) {

            for (DriverSessionReportDto record : data) {
                // Safely handle nulls for the detection fields
                String eventId = record.eventId() != null ? record.eventId().toString() : "N/A";
                String eventTime = record.eventTimestamp() != null ? record.eventTimestamp() : "N/A";
                String type = record.detectionType() != null ? record.detectionType() : "None";
                String severity = record.severity() != null ? record.severity() : "N/A";
                String value = record.valueDetected() != null ? record.valueDetected().toString() : "N/A";

                csvPrinter.printRecord(
                        record.driverId(),
                        record.driverFullName(),
                        record.sessionId(),
                        record.startTime(),
                        record.endTime(),
                        record.durationHours(),
                        record.totalAlerts(),
                        eventId,
                        eventTime,
                        type,
                        severity,
                        value
                );
            }

            csvPrinter.flush();

        } catch (IOException e) {
            throw new RuntimeException("Failed to generate CSV report", e);
        }
    }
}