package com.yaqazah.detection.service;

import com.yaqazah.detection.dto.DetectionRequest;
import com.yaqazah.detection.model.DetectionLog;
import com.yaqazah.detection.repository.DetectionLogRepository;
import com.yaqazah.session.model.Session;
import com.yaqazah.session.repository.SessionRepository;
import com.yaqazah.user.model.User;
import com.yaqazah.infrastructure.storage.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DetectionService {

    @Autowired private DetectionLogRepository detectionLogRepo;
    @Autowired private SessionRepository sessionRepo;
    @Autowired private FileService fileService;

    @Transactional
    public void processDetection(DetectionRequest request, User user) {
        // 1. Fetch the Session object to maintain Foreign Key integrity
        Session session = sessionRepo.findById(request.getSessionId())
                .orElseThrow(() -> new RuntimeException("Session not found"));

        // 2. Upload and generate URL
        String fileName = "event_" + user.getUserId() + "_" + System.currentTimeMillis() + ".jpg";
        String photoUrl = fileService.uploadBase64(request.getPhotoBase64(), fileName);

        // 3. Save Log
        DetectionLog log = new DetectionLog();
        log.setSession(session);
        log.setUser(user);
        log.setTimestamp(request.getTimestamp());
        log.setType(request.getType());
        log.setSeverity(request.getSeverity());
        log.setValueDetected(request.getValueDetected());
        log.setSnapshotUrl(photoUrl);

        detectionLogRepo.save(log);
    }
}