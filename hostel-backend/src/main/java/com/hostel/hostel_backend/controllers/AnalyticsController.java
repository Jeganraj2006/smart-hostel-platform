package com.hostel.hostel_backend.controllers;

import com.hostel.hostel_backend.exceptions.ResourceNotFoundException;
import com.hostel.hostel_backend.models.Complaint;
import com.hostel.hostel_backend.models.Fee;
import com.hostel.hostel_backend.models.Leave;
import com.hostel.hostel_backend.models.Room;
import com.hostel.hostel_backend.models.User;
import com.hostel.hostel_backend.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;

@RestController
@RequestMapping("/api/analytics")
@CrossOrigin(origins = "*")
public class AnalyticsController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private ComplaintRepository complaintRepository;

    @Autowired
    private LeaveRepository leaveRepository;

    @Autowired
    private FeeRepository feeRepository;

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    private void verifyAccess() {
        User user = getCurrentUser();
        String role = user.getRole();
        if (!"SUPER_ADMIN".equals(role) && !"ADMIN".equals(role) && !"HOD".equals(role)) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied: Only SUPER_ADMIN, ADMIN, or HOD can access this resource.");
        }
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<?> handleAccessDenied(org.springframework.security.access.AccessDeniedException ex) {
        Map<String, String> err = new HashMap<>();
        err.put("error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(err);
    }

    /**
     * GET /api/analytics/occupancy
     * Returns occupancy status (filled vs available beds) grouped by blockName.
     */
    @GetMapping("/occupancy")
    public ResponseEntity<?> getOccupancy() {
        verifyAccess();

        List<Room> rooms = roomRepository.findAll();
        Map<String, Map<String, Object>> blockStats = new HashMap<>();

        for (Room room : rooms) {
            String block = room.getBlockName() != null ? room.getBlockName() : "Unknown";
            blockStats.putIfAbsent(block, new HashMap<>());
            Map<String, Object> stats = blockStats.get(block);

            int capacity = room.getCapacity() != null ? room.getCapacity() : 0;
            int filled = room.getOccupantIds() != null ? room.getOccupantIds().size() : 0;
            int available = Math.max(0, capacity - filled);

            stats.put("filled", (int) stats.getOrDefault("filled", 0) + filled);
            stats.put("available", (int) stats.getOrDefault("available", 0) + available);
            stats.put("totalCapacity", (int) stats.getOrDefault("totalCapacity", 0) + capacity);
            stats.put("totalRooms", (int) stats.getOrDefault("totalRooms", 0) + 1);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, Map<String, Object>> entry : blockStats.entrySet()) {
            Map<String, Object> item = new HashMap<>(entry.getValue());
            item.put("blockName", entry.getKey());
            result.add(item);
        }

        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/analytics/complaint-heatmap
     * Returns complaint count grouped by blockName + category over the last 90 days.
     */
    @GetMapping("/complaint-heatmap")
    public ResponseEntity<?> getComplaintHeatmap() {
        verifyAccess();

        // Build student-to-block lookup mapping from Room data (highly performant single-pass)
        List<Room> allRooms = roomRepository.findAll();
        Map<String, String> studentToBlockMap = new HashMap<>();
        for (Room room : allRooms) {
            if (room.getOccupantIds() != null && room.getBlockName() != null) {
                for (String occupantId : room.getOccupantIds()) {
                    studentToBlockMap.put(occupantId, room.getBlockName());
                }
            }
        }

        LocalDateTime ninetyDaysAgo = LocalDateTime.now().minusDays(90);
        List<Complaint> complaints = complaintRepository.findByRaisedAtAfter(ninetyDaysAgo);

        // Group counts by blockName + category
        Map<String, Map<String, Integer>> nestedCounts = new HashMap<>();

        for (Complaint complaint : complaints) {
            String block = studentToBlockMap.getOrDefault(complaint.getStudentId(), "Unassigned");
            String category = complaint.getCategory() != null ? complaint.getCategory() : "GENERAL";

            nestedCounts.putIfAbsent(block, new HashMap<>());
            Map<String, Integer> catCounts = nestedCounts.get(block);
            catCounts.put(category, catCounts.getOrDefault(category, 0) + 1);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, Map<String, Integer>> blockEntry : nestedCounts.entrySet()) {
            for (Map.Entry<String, Integer> catEntry : blockEntry.getValue().entrySet()) {
                Map<String, Object> item = new HashMap<>();
                item.put("blockName", blockEntry.getKey());
                item.put("category", catEntry.getKey());
                item.put("count", catEntry.getValue());
                result.add(item);
            }
        }

        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/analytics/leave-patterns
     * Returns count of leave requests grouped by day of the week and by leave type.
     */
    @GetMapping("/leave-patterns")
    public ResponseEntity<?> getLeavePatterns() {
        verifyAccess();

        List<Leave> leaves = leaveRepository.findAll();

        Map<String, Integer> byDayOfWeek = new HashMap<>();
        Map<String, Integer> byLeaveType = new HashMap<>();

        // Initialize day of week counters to ensure they are all present
        for (java.time.DayOfWeek day : java.time.DayOfWeek.values()) {
            byDayOfWeek.put(day.toString(), 0);
        }

        for (Leave leave : leaves) {
            if (leave.getAppliedAt() != null) {
                String day = leave.getAppliedAt().getDayOfWeek().toString();
                byDayOfWeek.put(day, byDayOfWeek.getOrDefault(day, 0) + 1);
            }
            if (leave.getLeaveType() != null) {
                String type = leave.getLeaveType();
                byLeaveType.put(type, byLeaveType.getOrDefault(type, 0) + 1);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("byDayOfWeek", byDayOfWeek);
        result.put("byLeaveType", byLeaveType);

        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/analytics/fee-forecast
     * Forecasts the collection: PENDING fees due in next 30 days vs historical on-time-payment rate.
     */
    @GetMapping("/fee-forecast")
    public ResponseEntity<?> getFeeForecast() {
        verifyAccess();

        LocalDate today = LocalDate.now();
        LocalDate limit = today.plusDays(30);

        List<Fee> pendingFees = feeRepository.findByStatus("PENDING");
        double pendingAmount = 0.0;

        for (Fee fee : pendingFees) {
            if (fee.getDueDate() != null) {
                try {
                    LocalDate due = LocalDate.parse(fee.getDueDate());
                    if (!due.isBefore(today) && !due.isAfter(limit)) {
                        pendingAmount += fee.getAmount() != null ? fee.getAmount() : 0.0;
                    }
                } catch (Exception e) {
                    // skip malformed
                }
            }
        }

        // Calculate historical on-time-payment rate across all finalized fees
        List<Fee> allFees = feeRepository.findAll();
        long totalScored = 0;
        long onTimeCount = 0;

        for (Fee fee : allFees) {
            String status = fee.getStatus();
            if ("OVERDUE".equals(status)) {
                totalScored++;
            } else if ("PAID".equals(status)) {
                totalScored++;
                if (fee.getPaidDate() != null && fee.getDueDate() != null) {
                    try {
                        LocalDate paid = LocalDate.parse(fee.getPaidDate());
                        LocalDate due = LocalDate.parse(fee.getDueDate());
                        if (!paid.isAfter(due)) {
                            onTimeCount++;
                        }
                    } catch (DateTimeParseException e) {
                        onTimeCount++; // fallback
                    }
                } else {
                    onTimeCount++; // default to on time if no dates are set
                }
            }
        }

        double onTimeRate = totalScored > 0 ? (double) onTimeCount / totalScored : 1.0;
        double projectedCollection = pendingAmount * onTimeRate;

        Map<String, Object> result = new HashMap<>();
        result.put("pendingAmountNext30Days", pendingAmount);
        result.put("historicalOnTimeRate", onTimeRate);
        result.put("projectedCollection", projectedCollection);
        result.put("totalScoredFeesCount", totalScored);
        result.put("onTimePaidFeesCount", onTimeCount);

        return ResponseEntity.ok(result);
    }
}
