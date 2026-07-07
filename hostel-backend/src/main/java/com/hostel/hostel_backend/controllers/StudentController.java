package com.hostel.hostel_backend.controllers;

import com.hostel.hostel_backend.models.RoommatePreferences;
import com.hostel.hostel_backend.models.User;
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
}
