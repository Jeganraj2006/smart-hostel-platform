package com.hostel.hostel_backend.controllers;

import com.hostel.hostel_backend.models.Fee;
import com.hostel.hostel_backend.models.User;
import com.hostel.hostel_backend.repositories.UserRepository;
import com.hostel.hostel_backend.repositories.FeeRepository;
import com.hostel.hostel_backend.exceptions.ResourceNotFoundException;
import com.hostel.hostel_backend.services.AuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
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

    /**
     * POST /api/fees/{id}/pay
     *
     * Marks the fee record as PAID for the authenticated student.
     * This endpoint represents a "successful payment callback" / "mark as paid" action.
     * It is intentionally simplified for a student project where a live payment gateway
     * is out of scope.
     *
     * --- PRODUCTION GATEWAY INTEGRATION POINT ---
     * In a real deployment, this logic would NOT be called directly by the student.
     * Instead, you would:
     *  1. Create a payment order via the gateway SDK (e.g. Razorpay Orders API or
     *     Stripe PaymentIntent API) and return the order/client-secret to the frontend.
     *  2. The frontend renders the gateway checkout widget; on success the gateway
     *     sends a signed webhook POST to a dedicated endpoint (e.g. /api/webhooks/razorpay
     *     or /api/webhooks/stripe).
     *  3. That webhook handler verifies the signature (HMAC-SHA256 for Razorpay,
     *     Stripe-Signature header for Stripe), then calls this same internal marking
     *     logic to transition the Fee to PAID.
     * --------------------------------------------
     */
    @PostMapping("/{id}/pay")
    public ResponseEntity<?> payFee(@PathVariable String id) {
        User student = getCurrentUser();

        Fee fee = feeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fee not found with id: " + id));

        // Ownership check: a STUDENT may only mark their own fee as paid.
        // WARDEN / ADMIN roles are exempt from this restriction (they use PUT /{id}/status).
        if ("STUDENT".equals(student.getRole()) && !student.getId().equals(fee.getStudentId())) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Access denied: you can only pay your own fee.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
        }

        // Guard: do not double-mark an already paid fee.
        if ("PAID".equals(fee.getStatus())) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Fee is already marked as PAID.");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
        }

        String previousStatus = fee.getStatus();
        fee.setStatus("PAID");
        fee.setPaidDate(LocalDate.now().toString());
        Fee saved = feeRepository.save(fee);

        // Audit trail
        Map<String, String> metadata = new HashMap<>();
        metadata.put("previousStatus", previousStatus);
        metadata.put("paidDate", fee.getPaidDate());
        metadata.put("feeType", fee.getFeeType());
        metadata.put("amount", String.valueOf(fee.getAmount()));
        auditService.log(
                student.getId(),
                student.getRole(),
                "FEE_PAID",
                "FEE",
                id,
                metadata
        );

        return ResponseEntity.ok(saved);
    }
}