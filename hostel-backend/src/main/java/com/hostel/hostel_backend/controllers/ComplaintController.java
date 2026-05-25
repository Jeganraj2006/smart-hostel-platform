package com.hostel.hostel_backend.controllers;

import com.hostel.hostel_backend.models.Complaint;
import com.hostel.hostel_backend.models.User;
import com.hostel.hostel_backend.repositories.ComplaintRepository;
import com.hostel.hostel_backend.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/complaints")
@CrossOrigin(origins = "*")
public class ComplaintController {

    @Autowired
    private ComplaintRepository complaintRepository;

    @Autowired
    private UserRepository userRepository;

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return userRepository.findByEmail(email).orElseThrow();
    }

    @PostMapping
    public ResponseEntity<?> raise(@RequestBody Complaint request) {
        try {
            User student = getCurrentUser();
            request.setStudentId(student.getId());
            request.setStudentName(student.getName());
            request.setStatus("OPEN");
            return ResponseEntity.ok(complaintRepository.save(request));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/my")
    public ResponseEntity<?> getMyComplaints() {
        try {
            User student = getCurrentUser();
            return ResponseEntity.ok(
                    complaintRepository.findByStudentId(student.getId())
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<?> getAll() {
        return ResponseEntity.ok(complaintRepository.findAll());
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable String id,
                                          @RequestBody Map<String, String> body) {
        try {
            Complaint c = complaintRepository.findById(id).orElseThrow();
            c.setStatus(body.get("status"));
            if (body.get("status").equals("RESOLVED"))
                c.setResolvedAt(LocalDateTime.now());
            return ResponseEntity.ok(complaintRepository.save(c));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}/rate")
    public ResponseEntity<?> rate(@PathVariable String id,
                                  @RequestBody Map<String, Integer> body) {
        try {
            Complaint c = complaintRepository.findById(id).orElseThrow();
            c.setRating(body.get("rating"));
            return ResponseEntity.ok(complaintRepository.save(c));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}