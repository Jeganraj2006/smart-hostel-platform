package com.hostel.hostel_backend.controllers;

import com.hostel.hostel_backend.exceptions.BadRequestException;
import com.hostel.hostel_backend.exceptions.ResourceNotFoundException;
import com.hostel.hostel_backend.models.Attendance;
import com.hostel.hostel_backend.models.Leave;
import com.hostel.hostel_backend.models.User;
import com.hostel.hostel_backend.repositories.AttendanceRepository;
import com.hostel.hostel_backend.repositories.LeaveRepository;
import com.hostel.hostel_backend.repositories.UserRepository;
import com.hostel.hostel_backend.models.Room;
import com.hostel.hostel_backend.repositories.RoomRepository;
import com.hostel.hostel_backend.services.AuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/gate")
@CrossOrigin(origins = "*")
public class AttendanceController {

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private LeaveRepository leaveRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private AuditService auditService;

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    @PostMapping("/scan")
    @PreAuthorize("hasAnyRole('SECURITY_GUARD', 'WARDEN')")
    public ResponseEntity<Attendance> scanGatepass(@RequestBody Map<String, String> body) {
        String leaveId = body.get("leaveId");
        if (leaveId == null || leaveId.trim().isEmpty()) {
            throw new BadRequestException("leaveId is required");
        }

        Leave leave = leaveRepository.findById(leaveId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found with id: " + leaveId));

        if (!"APPROVED".equals(leave.getStatus())) {
            throw new BadRequestException("Gatepass scan is only allowed for APPROVED leaves. Current status: " + leave.getStatus());
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime fromDateParsed;
        LocalDateTime toDateParsed;

        try {
            if (leave.getFromDate().contains("T")) {
                fromDateParsed = LocalDateTime.parse(leave.getFromDate());
            } else {
                fromDateParsed = java.time.LocalDate.parse(leave.getFromDate()).atStartOfDay();
            }

            if (leave.getToDate().contains("T")) {
                toDateParsed = LocalDateTime.parse(leave.getToDate());
            } else {
                toDateParsed = java.time.LocalDate.parse(leave.getToDate()).atTime(23, 59, 59);
            }
        } catch (Exception e) {
            throw new BadRequestException("Invalid date format in leave request dates");
        }

        if (now.isBefore(fromDateParsed) || now.isAfter(toDateParsed)) {
            throw new BadRequestException("Leave is not active. Current time is outside the approved leave window: " 
                    + leave.getFromDate() + " to " + leave.getToDate());
        }

        User actor = getCurrentUser();
        Optional<Attendance> existingOpt = attendanceRepository.findByLeaveId(leaveId);
        Attendance attendance;

        if (existingOpt.isEmpty()) {
            // Exit scan
            attendance = new Attendance();
            attendance.setLeaveId(leaveId);
            attendance.setStudentId(leave.getStudentId());
            attendance.setExitScannedAt(now);
            attendance.setExpectedReturnAt(toDateParsed);
            attendance.setStatus("OUT");

            Attendance saved = attendanceRepository.save(attendance);

            // Audit Log
            Map<String, String> metadata = new HashMap<>();
            metadata.put("studentId", leave.getStudentId());
            metadata.put("scannedBy", actor.getName());
            auditService.log(
                actor.getId(),
                actor.getRole(),
                "GATE_EXIT",
                "ATTENDANCE",
                saved.getId(),
                metadata
            );

            return ResponseEntity.ok(saved);
        } else {
            attendance = existingOpt.get();
            if (!"OUT".equals(attendance.getStatus())) {
                throw new BadRequestException("Student has already returned for this leave. Current status: " + attendance.getStatus());
            }

            // Entry scan
            attendance.setEntryScannedAt(now);
            attendance.setStatus("RETURNED");

            Attendance saved = attendanceRepository.save(attendance);

            // Audit Log
            Map<String, String> metadata = new HashMap<>();
            metadata.put("studentId", leave.getStudentId());
            metadata.put("scannedBy", actor.getName());
            auditService.log(
                actor.getId(),
                actor.getRole(),
                "GATE_ENTRY",
                "ATTENDANCE",
                saved.getId(),
                metadata
            );

            return ResponseEntity.ok(saved);
        }
    }

    @GetMapping("/status")
    @PreAuthorize("hasAnyRole('SECURITY_GUARD', 'WARDEN')")
    public ResponseEntity<?> getGateStatus() {
        LocalDateTime startOfToday = LocalDateTime.now().with(java.time.LocalTime.MIN);
        java.util.List<Attendance> todaysScans = attendanceRepository.findByExitScannedAtAfterAndStatusIn(
                startOfToday, java.util.List.of("OUT", "OVERDUE")
        );

        java.util.List<Map<String, Object>> richList = new java.util.ArrayList<>();
        for (Attendance att : todaysScans) {
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("attendance", att);

            Optional<User> studentOpt = userRepository.findById(att.getStudentId());
            map.put("studentName", studentOpt.map(User::getName).orElse("Unknown Student"));
            map.put("studentEmail", studentOpt.map(User::getEmail).orElse("Unknown Email"));
            map.put("studentPhone", studentOpt.map(User::getPhone).orElse("N/A"));

            Optional<Room> roomOpt = roomRepository.findByOccupantIdsContaining(att.getStudentId());
            map.put("roomNo", roomOpt.map(Room::getRoomNumber).orElse("N/A"));
            map.put("blockName", roomOpt.map(Room::getBlockName).orElse("N/A"));

            richList.add(map);
        }
        return ResponseEntity.ok(richList);
    }
}
