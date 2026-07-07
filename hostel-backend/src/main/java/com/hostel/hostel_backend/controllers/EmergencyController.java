package com.hostel.hostel_backend.controllers;

import com.hostel.hostel_backend.exceptions.BadRequestException;
import com.hostel.hostel_backend.exceptions.ResourceNotFoundException;
import com.hostel.hostel_backend.models.Room;
import com.hostel.hostel_backend.models.User;
import com.hostel.hostel_backend.repositories.RoomRepository;
import com.hostel.hostel_backend.repositories.UserRepository;
import com.hostel.hostel_backend.services.AuditService;
import com.hostel.hostel_backend.services.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/emergency")
@CrossOrigin(origins = "*")
public class EmergencyController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private AuditService auditService;

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    public static class BroadcastRequest {
        private String scope; // "BLOCK" or "HOSTEL"
        private String blockName; // optional, required only if scope is "BLOCK"
        private String message;

        public String getScope() { return scope; }
        public void setScope(String scope) { this.scope = scope; }
        public String getBlockName() { return blockName; }
        public void setBlockName(String blockName) { this.blockName = blockName; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    @PostMapping("/broadcast")
    public ResponseEntity<?> broadcast(@RequestBody BroadcastRequest req) {
        User actor = getCurrentUser();
        String role = actor.getRole();

        // 1. Authorize WARDEN or ADMIN only
        if (!"WARDEN".equals(role) && !"ADMIN".equals(role) && !"SUPER_ADMIN".equals(role)) {
            return ResponseEntity.status(403).body(Map.of("error", "Access denied: WARDEN/ADMIN only"));
        }

        // 2. Validate request parameters
        if (req.getScope() == null || (!"BLOCK".equals(req.getScope()) && !"HOSTEL".equals(req.getScope()))) {
            throw new BadRequestException("Invalid scope. Must be BLOCK or HOSTEL");
        }
        if (req.getMessage() == null || req.getMessage().trim().isEmpty()) {
            throw new BadRequestException("Message content is required");
        }
        if ("BLOCK".equals(req.getScope()) && (req.getBlockName() == null || req.getBlockName().trim().isEmpty())) {
            throw new BadRequestException("blockName is required when scope is BLOCK");
        }

        // 3. Find active students
        List<User> students = userRepository.findByRole("STUDENT").stream()
                .filter(u -> "ACTIVE".equals(u.getAccountStatus()) && u.isActive())
                .collect(Collectors.toList());

        List<User> targetStudents;

        if ("BLOCK".equals(req.getScope())) {
            // Find rooms assigned to this block name
            List<Room> rooms = roomRepository.findByBlockName(req.getBlockName());
            Set<String> occupantIds = rooms.stream()
                    .flatMap(r -> r.getOccupantIds().stream())
                    .collect(Collectors.toSet());

            // Filter active students allocated to these rooms
            targetStudents = students.stream()
                    .filter(u -> occupantIds.contains(u.getId()))
                    .collect(Collectors.toList());
        } else {
            // "HOSTEL" scope -> broadcast to all active students
            targetStudents = students;
        }

        // 4. Send alert to target students (and their parents once linkage exists)
        for (User student : targetStudents) {
            // Send alert to student
            notificationService.sendAlert(student.getEmail(), req.getMessage());
            
            // TODO: Sprint 30: When parent linkage is added, lookup parents for each matching student and send alerts
            // example: parentService.findParentsForStudent(student.getId()).forEach(p -> notificationService.sendAlert(p.getEmail(), req.getMessage()));
        }

        // 5. Log audit trail
        Map<String, String> metadata = new HashMap<>();
        metadata.put("scope", req.getScope());
        if (req.getBlockName() != null) {
            metadata.put("blockName", req.getBlockName());
        }
        metadata.put("message", req.getMessage());
        metadata.put("recipientCount", String.valueOf(targetStudents.size()));

        auditService.log(
                actor.getId(),
                actor.getRole(),
                "EMERGENCY_BROADCAST",
                "EMERGENCY",
                null,
                metadata
        );

        return ResponseEntity.ok(Map.of(
                "message", "Broadcast sent successfully",
                "recipientCount", targetStudents.size()
        ));
    }
}
