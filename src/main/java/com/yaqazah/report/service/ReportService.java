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

//    @Autowired
//    private UserRepository userRepository;
//
//    public void generateCSVReport(Writer writer, UUID companyId) {
//
//        List<DriverSessionReportDto> data = userRepository.findCombinedDriverDataByCompany(companyId);
//
//        CSVFormat format = CSVFormat.DEFAULT.builder()
//                .setHeader("Driver ID", "Driver Name", "Session ID", "Start Time", "End Time",
//                        "Duration (Hrs)", "Total Alerts", "Event ID", "Event Time", "Alert Type",
//                        "Severity", "Value Detected")
//                .build();
//
//        try (CSVPrinter csvPrinter = new CSVPrinter(writer, format)) {
//
//            for (DriverSessionReportDto record : data) {
//
//                // Convert ALL Instants to Strings safely
//                String startStr = record.getStartTime() != null ? record.getStartTime().toString() : "N/A";
//                String endStr = record.getEndTime() != null ? record.getEndTime().toString() : "N/A";
//                String eventTimeStr = record.getEventTimestamp() != null ? record.getEventTimestamp().toString() : "N/A";
//
//                // Handle other nulls
//                String eventId = record.getEventId() != null ? record.getEventId().toString() : "N/A";
//                String type = record.getDetectionType() != null ? record.getDetectionType() : "None";
//                String severity = record.getSeverity() != null ? record.getSeverity() : "N/A";
//                String value = record.getValueDetected() != null ? record.getValueDetected().toString() : "N/A";
//
//                csvPrinter.printRecord(
//                        record.getDriverId(),
//                        record.getDriverFullName(),
//                        record.getSessionId(),
//                        startStr,            // Converted
//                        endStr,              // Converted
//                        record.getDurationHours(),
//                        record.getTotalAlerts(),
//                        eventId,
//                        eventTimeStr,        // Converted
//                        type,
//                        severity,
//                        value
//                );
//            }
//
//            csvPrinter.flush();
//
//        } catch (IOException e) {
//            throw new RuntimeException("Failed to generate CSV report", e);
//        }
//    }
}