package com.hostel.hostel_backend.controllers;

import com.hostel.hostel_backend.models.Fee;
import com.hostel.hostel_backend.models.User;
import com.hostel.hostel_backend.repositories.UserRepository;
import com.hostel.hostel_backend.repositories.FeeRepository;
import com.hostel.hostel_backend.exceptions.ResourceNotFoundException;
import com.hostel.hostel_backend.services.AuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/fees")
@CrossOrigin(origins = "*")
public class FeeController {

    @Autowired
    private FeeRepository feeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditService auditService;

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    @GetMapping("/my")
    public ResponseEntity<?> getMyFees() {
        User user = getCurrentUser();
        return ResponseEntity.ok(feeRepository.findByStudentId(user.getId()));
    }

    @GetMapping
    public ResponseEntity<?> getAll() {
        return ResponseEntity.ok(feeRepository.findAll());
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable String id,
                                          @RequestBody Map<String, String> body) {
        Fee fee = feeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fee not found with id: " + id));
        String oldStatus = fee.getStatus();
        String newStatus = body.get("status");
        fee.setStatus(newStatus);
        if ("PAID".equals(newStatus)) {
            fee.setPaidDate(java.time.LocalDate.now().toString());
        }
        Fee saved = feeRepository.save(fee);

        // Audit Logging
        User actor = getCurrentUser();
        Map<String, String> metadata = new HashMap<>();
        metadata.put("oldStatus", oldStatus);
        metadata.put("newStatus", newStatus);
        auditService.log(
            actor.getId(),
            actor.getRole(),
            "UPDATE_STATUS",
            "FEE",
            id,
            metadata
        );

        return ResponseEntity.ok(saved);
    }
}