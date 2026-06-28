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

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader("Driver ID", "Driver Name", "Session ID", "Start Time", "End Time",
                        "Duration (Hrs)", "Total Alerts", "Event ID", "Event Time",
                        "Alert ID", "Risk ID", "Title", "Subtitle")
                .build();

        try (CSVPrinter csvPrinter = new CSVPrinter(writer, format)) {

            for (DriverSessionReportDto record : data) {
                String eventId = record.eventId() != null ? record.eventId().toString() : "N/A";
                String eventTime = record.eventTimestamp() != null ? record.eventTimestamp() : "N/A";
                String alertId = record.alertId() != null ? record.alertId().toString() : "N/A";
                String riskId = record.riskId() != null ? record.riskId().toString() : "N/A";
                String title = record.title() != null ? record.title() : "N/A";
                String subtitle = record.subtitle() != null ? record.subtitle() : "N/A";

                csvPrinter.printRecord(
                        record.driverId(),
                        record.driverFullName(),
                        record.sessionId(),
                        record.startDateTime(),
                        record.endDateTime(),
                        record.durationHours(),
                        record.totalAlerts(),
                        eventId,
                        eventTime,
                        alertId,
                        riskId,
                        title,
                        subtitle
                );
            }

            csvPrinter.flush();

        } catch (IOException e) {
            throw new RuntimeException("Failed to generate CSV report", e);
        }
    }
}