package com.hostel.hostel_backend.controllers;

import com.hostel.hostel_backend.models.RoommatePreferences;
import com.hostel.hostel_backend.models.User;
import com.hostel.hostel_backend.repositories.RoomRepository;
import com.hostel.hostel_backend.repositories.UserRepository;
import com.hostel.hostel_backend.exceptions.BadRequestException;
import com.hostel.hostel_backend.exceptions.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/students")
@CrossOrigin(origins = "*")
public class StudentController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoomRepository roomRepository;

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    @GetMapping("/preferences")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> getPreferences() {
        User user = getCurrentUser();
        RoommatePreferences pref = user.getRoommatePreferences();
        return ResponseEntity.ok(Map.of(
            "hasPreferences", pref != null,
            "preferences", pref != null ? pref : new RoommatePreferences()
        ));
    }

    @PutMapping("/preferences")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> submitPreferences(@RequestBody RoommatePreferences preferences) {
        User user = getCurrentUser();

        // 1. Verify account is approved (ACTIVE)
        if (!"ACTIVE".equals(user.getAccountStatus())) {
            throw new BadRequestException("Preferences can only be submitted after registration approval");
        }

        // 2. Verify preferences have not already been submitted
        if (user.getRoommatePreferences() != null) {
            throw new BadRequestException("Roommate preferences have already been submitted");
        }

        // 3. Simple input validation
        if (preferences == null) {
            throw new BadRequestException("Preferences body is required");
        }

        String sleep = preferences.getSleepSchedule();
        if (!"EARLY_BIRD".equals(sleep) && !"NIGHT_OWL".equals(sleep)) {
            throw new BadRequestException("Invalid sleepSchedule value. Must be EARLY_BIRD or NIGHT_OWL");
        }

        Integer clean = preferences.getCleanlinessLevel();
        if (clean == null || clean < 1 || clean > 5) {
            throw new BadRequestException("Invalid cleanlinessLevel value. Must be between 1 and 5");
        }

        String study = preferences.getStudyHabit();
        if (!"SILENT".equals(study) && !"MUSIC_OK".equals(study) && !"GROUP_STUDY".equals(study)) {
            throw new BadRequestException("Invalid studyHabit value. Must be SILENT, MUSIC_OK, or GROUP_STUDY");
        }

        String lang = preferences.getPreferredLanguage();
        if (lang == null || lang.trim().isEmpty()) {
            throw new BadRequestException("preferredLanguage is required");
        }

        // 4. Save preferences
        user.setRoommatePreferences(preferences);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Roommate preferences submitted successfully"));
    }

    @DeleteMapping("/{id}/data")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<?> anonymizeStudentData(@PathVariable String id) {
        User actor = getCurrentUser();
        String actorRole = actor.getRole();
        if (!"SUPER_ADMIN".equals(actorRole) && !"ADMIN".equals(actorRole)) {
            return ResponseEntity.status(403).body(Map.of("error", "Access denied: Only SUPER_ADMIN or ADMIN can anonymize student data."));
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with ID: " + id));

        if (!"STUDENT".equals(user.getRole())) {
            throw new BadRequestException("Only STUDENT role accounts can be anonymized.");
        }

        // Anonymize PII details to comply with India's DPDP Act 2023 compliance while maintaining audit history keys
        user.setName("Anonymized Student");
        user.setPhone("0000000000");
        user.setEmail("anonymized_" + id + "@hostel.internal");
        user.setPassword("ANONYMIZED_" + java.util.UUID.randomUUID().toString());
        user.setRoommatePreferences(null);
        user.setChildEmailOrId(null);
        user.setActive(false);
        user.setAccountStatus("ANONYMIZED");

        userRepository.save(user);

        // Remove from roommate allocations if checked into any room
        roomRepository.findByOccupantIdsContaining(id).ifPresent(room -> {
            room.getOccupantIds().remove(id);
            if (room.getOccupantIds().isEmpty()) {
                room.setStatus("AVAILABLE");
            }
            roomRepository.save(room);
        });

        return ResponseEntity.ok(Map.of("message", "Student data successfully anonymized to preserve audit integrity under DPDP Act 2023 compliance."));
    }
}
