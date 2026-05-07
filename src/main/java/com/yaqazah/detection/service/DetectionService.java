package com.yaqazah.detection.service;

import com.yaqazah.detection.model.DetectionLog;
import com.yaqazah.detection.dto.DetectionRequest; // Added import
import com.yaqazah.detection.repository.DetectionLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Base64;

@Service
public class DetectionService {

    @Autowired
    private DetectionLogRepository detectionLogRepo;

    public void processDetection(DetectionRequest dto) {
        DetectionLog log = new DetectionLog();
        log.setSessionId(dto.getSessionId());
        log.setTimestamp(dto.getTimestamp());
        log.setType(dto.getType());
        log.setValueDetected(dto.getValueDetected());

        // Logic for severity
        log.setSeverity(dto.getValueDetected() > 0.7 ? "HIGH" : "NORMAL");

        // Convert the string to the efficient byte array
        if (dto.getPhotoBase64() != null) {
            byte[] imageBytes = Base64.getDecoder().decode(dto.getPhotoBase64());
            log.setSnapshotImage(imageBytes); // This now matches the model!
        }

        detectionLogRepo.save(log);
    }
}