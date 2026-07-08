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
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> apply(@RequestBody LeaveRequest request) {
        Leave leave = leaveService.applyLeave(request);
        return ResponseEntity.ok(leave);
    }

    @GetMapping("/my")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> getMyLeaves() {
        return ResponseEntity.ok(leaveService.getMyLeaves());
    }

    @GetMapping("/pending")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('WARDEN', 'HOD', 'PARENT')")
    public ResponseEntity<?> getPending() {
        return ResponseEntity.ok(leaveService.getPendingLeaves());
    }

    @PutMapping("/{id}/approve")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('WARDEN', 'HOD', 'PARENT')")
    public ResponseEntity<?> approve(@PathVariable String id,
                                     @RequestBody Map<String, Integer> body) {
        Leave leave = leaveService.approveLeave(id, body.get("level"));
        return ResponseEntity.ok(leave);
    }

    @PutMapping("/{id}/reject")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('WARDEN', 'HOD', 'PARENT')")
    public ResponseEntity<?> reject(@PathVariable String id,
                                    @RequestBody Map<String, String> body) {
        Leave leave = leaveService.rejectLeave(id, body.get("reason"));
        return ResponseEntity.ok(leave);
    }

    @PostMapping("/{id}/emergency-override")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('WARDEN')")
    public ResponseEntity<?> emergencyOverride(@PathVariable String id,
                                               @RequestBody Map<String, String> body) {
        Leave leave = leaveService.emergencyOverride(id, body.get("reason"));
        return ResponseEntity.ok(leave);
    }

    @PostMapping("/{id}/reminder")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> sendReminder(@PathVariable String id) {
        leaveService.sendReminder(id);
        return ResponseEntity.ok("Reminder sent");
    }
}