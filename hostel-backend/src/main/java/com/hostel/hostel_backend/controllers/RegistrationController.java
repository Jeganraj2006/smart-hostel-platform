package com.hostel.hostel_backend.controllers;

import com.hostel.hostel_backend.models.User;
import com.hostel.hostel_backend.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/registrations")
@CrossOrigin(origins = "*")
public class RegistrationController {

    @Autowired
    private UserRepository userRepository;

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
        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            user.setAccountStatus("ACTIVE");
            user.setApprovedAt(LocalDateTime.now());
            userRepository.save(user);
            return ResponseEntity.ok("User approved successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Warden rejects a registration
    @PutMapping("/{id}/reject")
    public ResponseEntity<?> reject(@PathVariable String id,
                                    @RequestBody Map<String, String> body) {
        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            user.setAccountStatus("REJECTED");
            user.setRejectionReason(body.get("reason"));
            userRepository.save(user);
            return ResponseEntity.ok("User rejected");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Get all active users
    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        List<User> users = userRepository.findAll();
        users.forEach(u -> u.setPassword(null));
        return ResponseEntity.ok(users);
    }
}