package com.hostel.hostel_backend.controllers;

import com.hostel.hostel_backend.exceptions.ResourceNotFoundException;
import com.hostel.hostel_backend.models.User;
import com.hostel.hostel_backend.models.Visitor;
import com.hostel.hostel_backend.repositories.UserRepository;
import com.hostel.hostel_backend.repositories.VisitorRepository;
import com.hostel.hostel_backend.services.AuditService;
import com.hostel.hostel_backend.services.CloudinaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * VisitorController — manages hostel visitor check-in, check-out, and photo upload.
 *
 * Endpoints:
 *   POST   /api/visitors              — check-in a visitor        (SECURITY_GUARD, WARDEN)
 *   PUT    /api/visitors/{id}/checkout — check-out a visitor      (SECURITY_GUARD, WARDEN)
 *   GET    /api/visitors/active        — all currently checked-in (SECURITY_GUARD, WARDEN)
 *   POST   /api/visitors/{id}/photo   — upload visitor photo      (SECURITY_GUARD, WARDEN)
 */
@RestController
@RequestMapping("/api/visitors")
@CrossOrigin(origins = "*")
public class VisitorController {

    @Autowired private VisitorRepository visitorRepository;
    @Autowired private UserRepository    userRepository;
    @Autowired private AuditService      auditService;
    @Autowired private CloudinaryService cloudinaryService;

    // ── Auth helper ────────────────────────────────────────────────────────

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }

    /** Returns 403 if the caller is not SECURITY_GUARD or WARDEN/SUPER_ADMIN. */
    private ResponseEntity<?> checkGuardOrWarden(User actor) {
        String role = actor.getRole();
        if (!"SECURITY_GUARD".equals(role) && !"WARDEN".equals(role) && !"SUPER_ADMIN".equals(role)) {
            Map<String, String> err = new HashMap<>();
            err.put("error", "Access denied: SECURITY_GUARD or WARDEN role required.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(err);
        }
        return null;   // null = access granted
    }

    // ── Request DTO (inner class keeps things tidy) ─────────────────────────

    public static class CheckInRequest {
        private String visitorName;
        private String visitorPhone;
        private String purpose;
        private String hostStudentId;

        public String getVisitorName()    { return visitorName; }
        public void setVisitorName(String visitorName) { this.visitorName = visitorName; }
        public String getVisitorPhone()   { return visitorPhone; }
        public void setVisitorPhone(String visitorPhone) { this.visitorPhone = visitorPhone; }
        public String getPurpose()        { return purpose; }
        public void setPurpose(String purpose) { this.purpose = purpose; }
        public String getHostStudentId()  { return hostStudentId; }
        public void setHostStudentId(String hostStudentId) { this.hostStudentId = hostStudentId; }
    }

    // ── POST /api/visitors — check-in ─────────────────────────────────────

    @PostMapping
    public ResponseEntity<?> checkIn(@RequestBody CheckInRequest req) {
        User actor = getCurrentUser();
        ResponseEntity<?> denied = checkGuardOrWarden(actor);
        if (denied != null) return denied;

        // Basic validation
        if (req.getVisitorName() == null || req.getVisitorName().isBlank()) {
            Map<String, String> err = new HashMap<>();
            err.put("error", "visitorName is required.");
            return ResponseEntity.badRequest().body(err);
        }

        Visitor v = new Visitor();
        v.setVisitorName(req.getVisitorName());
        v.setVisitorPhone(req.getVisitorPhone());
        v.setPurpose(req.getPurpose());
        v.setHostStudentId(req.getHostStudentId());
        v.setApprovedBy(actor.getId());
        v.setCheckInAt(LocalDateTime.now());
        Visitor saved = visitorRepository.save(v);

        // Audit
        Map<String, String> meta = new HashMap<>();
        meta.put("visitorName",    saved.getVisitorName());
        meta.put("visitorPhone",   saved.getVisitorPhone() != null ? saved.getVisitorPhone() : "—");
        meta.put("purpose",        saved.getPurpose() != null ? saved.getPurpose() : "—");
        meta.put("hostStudentId",  saved.getHostStudentId() != null ? saved.getHostStudentId() : "—");
        auditService.log(actor.getId(), actor.getRole(), "VISITOR_CHECK_IN", "VISITOR", saved.getId(), meta);

        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // ── PUT /api/visitors/{id}/checkout ────────────────────────────────────

    @PutMapping("/{id}/checkout")
    public ResponseEntity<?> checkOut(@PathVariable String id) {
        User actor = getCurrentUser();
        ResponseEntity<?> denied = checkGuardOrWarden(actor);
        if (denied != null) return denied;

        Visitor v = visitorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Visitor not found: " + id));

        if (v.getCheckOutAt() != null) {
            Map<String, String> err = new HashMap<>();
            err.put("error", "Visitor has already checked out.");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(err);
        }

        v.setCheckOutAt(LocalDateTime.now());
        Visitor saved = visitorRepository.save(v);

        // Audit
        Map<String, String> meta = new HashMap<>();
        meta.put("visitorName", saved.getVisitorName());
        meta.put("checkInAt",   saved.getCheckInAt().toString());
        meta.put("checkOutAt",  saved.getCheckOutAt().toString());
        auditService.log(actor.getId(), actor.getRole(), "VISITOR_CHECK_OUT", "VISITOR", id, meta);

        return ResponseEntity.ok(saved);
    }

    // ── GET /api/visitors/active ───────────────────────────────────────────

    /**
     * Returns all visitors that have not yet checked out (checkOutAt is null).
     * Must appear before the /{id}/checkout mapping to avoid path-variable collision.
     */
    @GetMapping("/active")
    public ResponseEntity<?> getActive() {
        User actor = getCurrentUser();
        ResponseEntity<?> denied = checkGuardOrWarden(actor);
        if (denied != null) return denied;

        List<Visitor> active = visitorRepository.findByCheckOutAtIsNull();
        return ResponseEntity.ok(active);
    }

    // ── POST /api/visitors/{id}/photo — optional photo upload ──────────────

    /**
     * Accepts a multipart image upload, pushes it to Cloudinary under the
     * "visitors/{visitorId}" folder, and stores the returned secure URL on
     * the visitor record's photoUrl field.
     *
     * This endpoint is intentionally separate from check-in so that the guard
     * can first create the record (fast) and attach the photo asynchronously.
     */
    @PostMapping(value = "/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadPhoto(@PathVariable String id,
                                         @RequestParam("file") MultipartFile file) {
        User actor = getCurrentUser();
        ResponseEntity<?> denied = checkGuardOrWarden(actor);
        if (denied != null) return denied;

        if (file == null || file.isEmpty()) {
            Map<String, String> err = new HashMap<>();
            err.put("error", "No file provided.");
            return ResponseEntity.badRequest().body(err);
        }

        Visitor v = visitorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Visitor not found: " + id));

        String photoUrl;
        try {
            photoUrl = cloudinaryService.uploadImage(file, "visitors/" + id);
        } catch (IOException e) {
            Map<String, String> err = new HashMap<>();
            err.put("error", "Photo upload failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
        }

        v.setPhotoUrl(photoUrl);
        Visitor saved = visitorRepository.save(v);

        // Audit
        Map<String, String> meta = new HashMap<>();
        meta.put("photoUrl", photoUrl);
        auditService.log(actor.getId(), actor.getRole(), "VISITOR_PHOTO_UPLOADED", "VISITOR", id, meta);

        return ResponseEntity.ok(saved);
    }
}
