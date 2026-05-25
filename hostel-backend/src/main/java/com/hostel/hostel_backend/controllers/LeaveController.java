package com.hostel.hostel_backend.controllers;

import com.hostel.hostel_backend.dto.LeaveRequest;
import com.hostel.hostel_backend.models.Leave;
import com.hostel.hostel_backend.services.LeaveService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/leaves")
@CrossOrigin(origins = "*")
public class LeaveController {

    @Autowired
    private LeaveService leaveService;

    @PostMapping("/apply")
    public ResponseEntity<?> apply(@RequestBody LeaveRequest request) {
        try {
            Leave leave = leaveService.applyLeave(request);
            return ResponseEntity.ok(leave);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/my")
    public ResponseEntity<?> getMyLeaves() {
        try {
            return ResponseEntity.ok(leaveService.getMyLeaves());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/pending")
    public ResponseEntity<?> getPending() {
        try {
            return ResponseEntity.ok(leaveService.getPendingLeaves());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<?> approve(@PathVariable String id,
                                     @RequestBody Map<String, Integer> body) {
        try {
            Leave leave = leaveService.approveLeave(id, body.get("level"));
            return ResponseEntity.ok(leave);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<?> reject(@PathVariable String id,
                                    @RequestBody Map<String, String> body) {
        try {
            Leave leave = leaveService.rejectLeave(id, body.get("reason"));
            return ResponseEntity.ok(leave);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{id}/emergency-override")
    public ResponseEntity<?> emergencyOverride(@PathVariable String id,
                                               @RequestBody Map<String, String> body) {
        try {
            Leave leave = leaveService.emergencyOverride(id, body.get("reason"));
            return ResponseEntity.ok(leave);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{id}/reminder")
    public ResponseEntity<?> sendReminder(@PathVariable String id) {
        try {
            leaveService.sendReminder(id);
            return ResponseEntity.ok("Reminder sent");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}