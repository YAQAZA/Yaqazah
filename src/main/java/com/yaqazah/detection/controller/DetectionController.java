package com.yaqazah.detection.controller;

import com.yaqazah.detection.dto.DetectionRequest;
import com.yaqazah.detection.model.DetectionLog;
import com.yaqazah.detection.repository.DetectionLogRepository;
import com.yaqazah.detection.service.DetectionService;
import com.yaqazah.user.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/detections")
@CrossOrigin(origins = "*")
public class DetectionController {

    @Autowired
    private DetectionService detectionService;

    @Autowired
    private DetectionLogRepository detectionLogRepo;

    @PreAuthorize("hasAnyRole('FLEET_DRIVER', 'INDEPENDENT_DRIVER')")
    @PostMapping("/log")
    public ResponseEntity<String> logDetection(
            @RequestBody DetectionRequest request,
            @AuthenticationPrincipal User user
    ) {
        try {
            detectionService.processDetection(request, user);
            return ResponseEntity.ok("Detection logged successfully");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/session/{sessionId}")
    public ResponseEntity<List<DetectionLog>> getSessionLogs(@PathVariable UUID sessionId) {
        return ResponseEntity.ok(detectionLogRepo.findBySession_SessionId(sessionId));
    }
}