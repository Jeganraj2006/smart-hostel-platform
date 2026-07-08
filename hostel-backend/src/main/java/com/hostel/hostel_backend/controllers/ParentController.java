package com.hostel.hostel_backend.controllers;

import com.hostel.hostel_backend.exceptions.BadRequestException;
import com.hostel.hostel_backend.exceptions.ResourceNotFoundException;
import com.hostel.hostel_backend.models.Room;
import com.hostel.hostel_backend.models.User;
import com.hostel.hostel_backend.repositories.RoomRepository;
import com.hostel.hostel_backend.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/parent")
@CrossOrigin(origins = "*")
public class ParentController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private com.hostel.hostel_backend.repositories.LeaveRepository leaveRepository;

    @Autowired
    private com.hostel.hostel_backend.repositories.FeeRepository feeRepository;

    @Autowired
    private com.hostel.hostel_backend.repositories.ComplaintRepository complaintRepository;

    @Autowired
    private com.hostel.hostel_backend.repositories.AttendanceRepository attendanceRepository;

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    /**
     * GET /api/parent/my-student
     * Returns the linked student's basic profile including room details.
     * Restricted to PARENT role only.
     */
    @GetMapping("/my-student")
    public ResponseEntity<?> getMyStudent() {
        User parent = getCurrentUser();

        // 1. Authorize PARENT only
        if (!"PARENT".equals(parent.getRole())) {
            Map<String, String> err = new HashMap<>();
            err.put("error", "Access denied: Only users with PARENT role can access this resource.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(err);
        }

        String studentId = parent.getLinkedStudentId();
        if (studentId == null || studentId.trim().isEmpty()) {
            throw new BadRequestException("No student is currently linked to this parent account.");
        }

        // 2. Fetch student details
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Linked student not found in the system."));

        // 3. Look up room details
        Optional<Room> roomOpt = roomRepository.findByOccupantIdsContaining(student.getId());

        Map<String, Object> response = new HashMap<>();
        response.put("id", student.getId());
        response.put("name", student.getName());
        response.put("email", student.getEmail());
        response.put("phone", student.getPhone());
        response.put("isActive", student.isActive());
        response.put("accountStatus", student.getAccountStatus());

        if (roomOpt.isPresent()) {
            Room r = roomOpt.get();
            response.put("roomNumber", r.getRoomNumber());
            response.put("blockName", r.getBlockName());
            response.put("roomType", r.getRoomType());
        } else {
            response.put("roomNumber", null);
            response.put("blockName", null);
            response.put("roomType", null);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/parent/timeline
     * Returns a chronologically sorted aggregated feed of leaves, fees, complaints, and gate activity.
     * Restricted to PARENT role only.
     */
    @GetMapping("/timeline")
    public ResponseEntity<?> getTimeline() {
        User parent = getCurrentUser();

        // 1. Authorize PARENT only
        if (!"PARENT".equals(parent.getRole())) {
            Map<String, String> err = new HashMap<>();
            err.put("error", "Access denied: Only users with PARENT role can access this resource.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(err);
        }

        String studentId = parent.getLinkedStudentId();
        if (studentId == null || studentId.trim().isEmpty()) {
            throw new BadRequestException("No student is currently linked to this parent account.");
        }

        java.util.List<Map<String, Object>> timeline = new java.util.ArrayList<>();

        // Fetch Leaves
        java.util.List<com.hostel.hostel_backend.models.Leave> leaves = leaveRepository.findByStudentIdOrderByAppliedAtDesc(studentId);
        for (com.hostel.hostel_backend.models.Leave leave : leaves) {
            Map<String, Object> item = new HashMap<>();
            item.put("type", "LEAVE");
            item.put("description", String.format("%s Leave to %s", leave.getLeaveType(), leave.getDestination()));
            item.put("status", leave.getStatus());
            item.put("timestamp", leave.getAppliedAt() != null ? leave.getAppliedAt() : java.time.LocalDateTime.now());
            timeline.add(item);
        }

        // Fetch Fees
        java.util.List<com.hostel.hostel_backend.models.Fee> fees = feeRepository.findByStudentId(studentId);
        for (com.hostel.hostel_backend.models.Fee fee : fees) {
            Map<String, Object> item = new HashMap<>();
            item.put("type", "FEE");
            item.put("description", String.format("%s Fee - INR %.2f", fee.getFeeType(), fee.getAmount()));
            item.put("status", fee.getStatus());
            
            java.time.LocalDateTime ts = java.time.LocalDateTime.now();
            try {
                if (fee.getPaidDate() != null && !fee.getPaidDate().trim().isEmpty()) {
                    ts = java.time.LocalDate.parse(fee.getPaidDate()).atStartOfDay();
                } else if (fee.getDueDate() != null && !fee.getDueDate().trim().isEmpty()) {
                    ts = java.time.LocalDate.parse(fee.getDueDate()).atStartOfDay();
                }
            } catch (Exception e) {
                // Keep default now
            }
            item.put("timestamp", ts);
            timeline.add(item);
        }

        // Fetch Complaints
        java.util.List<com.hostel.hostel_backend.models.Complaint> complaints = complaintRepository.findByStudentId(studentId);
        for (com.hostel.hostel_backend.models.Complaint complaint : complaints) {
            Map<String, Object> item = new HashMap<>();
            item.put("type", "COMPLAINT");
            item.put("description", String.format("%s complaint raised: %s", complaint.getCategory(), complaint.getDescription()));
            item.put("status", complaint.getStatus());
            item.put("timestamp", complaint.getRaisedAt() != null ? complaint.getRaisedAt() : java.time.LocalDateTime.now());
            timeline.add(item);
        }

        // Fetch Attendance (Gate Moves)
        java.util.List<com.hostel.hostel_backend.models.Attendance> attendances = attendanceRepository.findByStudentId(studentId);
        for (com.hostel.hostel_backend.models.Attendance att : attendances) {
            // Exit Scanned
            if (att.getExitScannedAt() != null) {
                Map<String, Object> exitItem = new HashMap<>();
                exitItem.put("type", "GATE_EXIT");
                exitItem.put("description", "Exited from the hostel gate");
                exitItem.put("status", "OUT".equals(att.getStatus()) ? "OUT" : ("OVERDUE".equals(att.getStatus()) && att.getEntryScannedAt() == null ? "OVERDUE" : "COMPLETED"));
                exitItem.put("timestamp", att.getExitScannedAt());
                timeline.add(exitItem);
            }
            // Entry Scanned
            if (att.getEntryScannedAt() != null) {
                Map<String, Object> entryItem = new HashMap<>();
                entryItem.put("type", "GATE_ENTRY");
                entryItem.put("description", "Returned to the hostel gate");
                entryItem.put("status", "RETURNED");
                entryItem.put("timestamp", att.getEntryScannedAt());
                timeline.add(entryItem);
            }
        }

        // Sort descending by timestamp
        timeline.sort((a, b) -> ((java.time.LocalDateTime) b.get("timestamp")).compareTo((java.time.LocalDateTime) a.get("timestamp")));

        // Convert java.time.LocalDateTime to String representation for JSON
        java.util.List<Map<String, Object>> responseList = new java.util.ArrayList<>();
        for (Map<String, Object> tItem : timeline) {
            Map<String, Object> formatted = new HashMap<>(tItem);
            formatted.put("timestamp", tItem.get("timestamp").toString());
            responseList.add(formatted);
        }

        return ResponseEntity.ok(responseList);
    }
}
