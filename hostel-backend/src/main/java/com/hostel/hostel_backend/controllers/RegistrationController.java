package com.hostel.hostel_backend.controllers;

import com.hostel.hostel_backend.models.User;
import com.hostel.hostel_backend.repositories.UserRepository;
import com.hostel.hostel_backend.exceptions.ResourceNotFoundException;
import com.hostel.hostel_backend.services.AuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/registrations")
@CrossOrigin(origins = "*")
public class RegistrationController {

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

    // Warden sees all pending registrations
    @GetMapping("/pending")
    public ResponseEntity<?> getPending() {
        List<User> pending = userRepository.findByAccountStatus("PENDING");
        // Hide passwords before sending
        pending.forEach(u -> u.setPassword(null));
        return ResponseEntity.ok(pending);
    }

    // Warden approves a registration
    @PutMapping("/{id}/approve")
    public ResponseEntity<?> approve(@PathVariable String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        user.setAccountStatus("ACTIVE");
        user.setApprovedAt(LocalDateTime.now());
        userRepository.save(user);

        // Audit Logging
        User actor = getCurrentUser();
        Map<String, String> metadata = new HashMap<>();
        metadata.put("approvedUserEmail", user.getEmail());
        auditService.log(
            actor.getId(),
            actor.getRole(),
            "APPROVE",
            "REGISTRATION",
            id,
            metadata
        );

        return ResponseEntity.ok("User approved successfully");
    }

    // Warden rejects a registration
    @PutMapping("/{id}/reject")
    public ResponseEntity<?> reject(@PathVariable String id,
                                    @RequestBody Map<String, String> body) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        user.setAccountStatus("REJECTED");
        user.setRejectionReason(body.get("reason"));
        userRepository.save(user);

        // Audit Logging
        User actor = getCurrentUser();
        Map<String, String> metadata = new HashMap<>();
        metadata.put("rejectedUserEmail", user.getEmail());
        metadata.put("reason", body.get("reason"));
        auditService.log(
            actor.getId(),
            actor.getRole(),
            "REJECT",
            "REGISTRATION",
            id,
            metadata
        );

        return ResponseEntity.ok("User rejected");
    }

    // Get all active users
    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        List<User> users = userRepository.findAll();
        users.forEach(u -> u.setPassword(null));
        return ResponseEntity.ok(users);
    }
}