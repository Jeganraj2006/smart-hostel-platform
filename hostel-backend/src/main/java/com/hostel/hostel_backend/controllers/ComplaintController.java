package com.hostel.hostel_backend.controllers;

import com.hostel.hostel_backend.models.Complaint;
import com.hostel.hostel_backend.models.User;
import com.hostel.hostel_backend.repositories.ComplaintRepository;
import com.hostel.hostel_backend.repositories.UserRepository;
import com.hostel.hostel_backend.exceptions.ResourceNotFoundException;
import com.hostel.hostel_backend.services.AuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/complaints")
@CrossOrigin(origins = "*")
public class ComplaintController {

    @Autowired
    private ComplaintRepository complaintRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditService auditService;

    @Autowired
    private com.hostel.hostel_backend.services.NlpTriageService nlpTriageService;

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    @PostMapping
    public ResponseEntity<?> raise(@RequestBody Complaint request) {
        User student = getCurrentUser();
        request.setStudentId(student.getId());
        request.setStudentName(student.getName());
        request.setStatus("OPEN");

        // Auto-triage category and priority if blank
        if (request.getCategory() == null || request.getCategory().trim().isEmpty()) {
            request.setCategory(nlpTriageService.triageCategory(request.getDescription()));
        }
        if (request.getPriority() == null || request.getPriority().trim().isEmpty()) {
            request.setPriority(nlpTriageService.triagePriority(request.getDescription()));
        }

        return ResponseEntity.ok(complaintRepository.save(request));
    }

    @GetMapping("/my")
    public ResponseEntity<?> getMyComplaints() {
        User student = getCurrentUser();
        return ResponseEntity.ok(
                complaintRepository.findByStudentId(student.getId())
        );
    }

    @GetMapping
    public ResponseEntity<?> getAll() {
        return ResponseEntity.ok(complaintRepository.findAll());
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable String id,
                                          @RequestBody Map<String, String> body) {
        Complaint c = complaintRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found with id: " + id));
        c.setStatus(body.get("status"));
        if (body.get("status").equals("RESOLVED"))
            c.setResolvedAt(LocalDateTime.now());
        Complaint saved = complaintRepository.save(c);

        // Audit Logging
        User actor = getCurrentUser();
        Map<String, String> metadata = new HashMap<>();
        metadata.put("status", body.get("status"));
        auditService.log(
            actor.getId(),
            actor.getRole(),
            "UPDATE_STATUS",
            "COMPLAINT",
            id,
            metadata
        );

        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}/rate")
    public ResponseEntity<?> rate(@PathVariable String id,
                                  @RequestBody Map<String, Integer> body) {
        Complaint c = complaintRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found with id: " + id));
        c.setRating(body.get("rating"));
        return ResponseEntity.ok(complaintRepository.save(c));
    }
}