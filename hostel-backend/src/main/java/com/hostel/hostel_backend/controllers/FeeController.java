package com.hostel.hostel_backend.controllers;

import com.hostel.hostel_backend.models.Fee;
import com.hostel.hostel_backend.models.User;
import com.hostel.hostel_backend.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Repository
interface FeeRepository extends MongoRepository<Fee, String> {
    List<Fee> findByStudentId(String studentId);
}

@RestController
@RequestMapping("/api/fees")
@CrossOrigin(origins = "*")
public class FeeController {

    @Autowired
    private FeeRepository feeRepository;

    @Autowired
    private UserRepository userRepository;

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return userRepository.findByEmail(email).orElseThrow();
    }

    @GetMapping("/my")
    public ResponseEntity<?> getMyFees() {
        try {
            User user = getCurrentUser();
            return ResponseEntity.ok(feeRepository.findByStudentId(user.getId()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<?> getAll() {
        return ResponseEntity.ok(feeRepository.findAll());
    }
}