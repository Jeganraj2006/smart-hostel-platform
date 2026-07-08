package com.hostel.hostel_backend.controllers;

import com.hostel.hostel_backend.exceptions.BadRequestException;
import com.hostel.hostel_backend.exceptions.ResourceNotFoundException;
import com.hostel.hostel_backend.models.ResourceLog;
import com.hostel.hostel_backend.models.User;
import com.hostel.hostel_backend.repositories.ResourceLogRepository;
import com.hostel.hostel_backend.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/resources")
@CrossOrigin(origins = "*")
public class ResourceController {

    @Autowired
    private ResourceLogRepository resourceLogRepository;

    @Autowired
    private UserRepository userRepository;

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    /**
     * POST /api/resources
     * Log a daily reading. Restricted to STAFF/WARDEN only.
     */
    @PostMapping
    public ResponseEntity<?> logResource(@RequestBody ResourceLog logReq) {
        User user = getCurrentUser();
        String role = user.getRole();

        if (!"STAFF".equals(role) && !"WARDEN".equals(role)) {
            Map<String, String> err = new HashMap<>();
            err.put("error", "Access denied: Only STAFF or WARDEN roles can log resource readings.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(err);
        }

        // Validations
        if (logReq.getDate() == null || logReq.getDate().trim().isEmpty()) {
            throw new BadRequestException("Date is required.");
        }
        if (logReq.getResourceType() == null || logReq.getResourceType().trim().isEmpty()) {
            throw new BadRequestException("ResourceType is required.");
        }
        String type = logReq.getResourceType().toUpperCase();
        if (!"ELECTRICITY".equals(type) && !"WATER".equals(type) && !"MESS_WASTAGE".equals(type)) {
            throw new BadRequestException("Invalid resourceType. Allowed values: ELECTRICITY, WATER, MESS_WASTAGE.");
        }
        if (logReq.getBlockName() == null || logReq.getBlockName().trim().isEmpty()) {
            throw new BadRequestException("BlockName is required.");
        }
        if (logReq.getQuantity() == null || logReq.getQuantity() < 0) {
            throw new BadRequestException("Quantity must be a positive number.");
        }
        if (logReq.getUnit() == null || logReq.getUnit().trim().isEmpty()) {
            throw new BadRequestException("Unit is required.");
        }

        logReq.setResourceType(type);
        logReq.setRecordedBy(user.getEmail());
        ResourceLog saved = resourceLogRepository.save(logReq);
        return ResponseEntity.ok(saved);
    }

    /**
     * GET /api/resources/summary
     * Returns monthly totals per block per resourceType. Restricted to ADMIN/HOD only.
     */
    @GetMapping("/summary")
    public ResponseEntity<?> getSummary() {
        User user = getCurrentUser();
        String role = user.getRole();

        if (!"SUPER_ADMIN".equals(role) && !"ADMIN".equals(role) && !"HOD".equals(role)) {
            Map<String, String> err = new HashMap<>();
            err.put("error", "Access denied: Only ADMIN or HOD roles can view resource summaries.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(err);
        }

        List<ResourceLog> logs = resourceLogRepository.findAll();
        
        // Group by Month (YYYY-MM), Block, ResourceType
        // structure: Map<Month, Map<Block, Map<ResourceType, Double>>>
        Map<String, Map<String, Map<String, Double>>> grouped = new HashMap<>();
        Map<String, String> unitMap = new HashMap<>(); // key: ResourceType, val: Unit

        for (ResourceLog log : logs) {
            if (log.getDate() == null || log.getDate().length() < 7) continue;
            String month = log.getDate().substring(0, 7); // "YYYY-MM"
            String block = log.getBlockName() != null ? log.getBlockName() : "Unknown";
            String type = log.getResourceType() != null ? log.getResourceType() : "Unknown";
            double qty = log.getQuantity() != null ? log.getQuantity() : 0.0;

            if (log.getResourceType() != null && log.getUnit() != null) {
                unitMap.put(log.getResourceType(), log.getUnit());
            }

            grouped.putIfAbsent(month, new HashMap<>());
            Map<String, Map<String, Double>> blockMap = grouped.get(month);

            blockMap.putIfAbsent(block, new HashMap<>());
            Map<String, Double> typeMap = blockMap.get(block);

            typeMap.put(type, typeMap.getOrDefault(type, 0.0) + qty);
        }

        // Format as List of Flat Maps for easier rendering
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, Map<String, Map<String, Double>>> monthEntry : grouped.entrySet()) {
            String month = monthEntry.getKey();
            for (Map.Entry<String, Map<String, Double>> blockEntry : monthEntry.getValue().entrySet()) {
                String block = blockEntry.getKey();
                for (Map.Entry<String, Double> typeEntry : blockEntry.getValue().entrySet()) {
                    String type = typeEntry.getKey();
                    double total = typeEntry.getValue();

                    Map<String, Object> item = new HashMap<>();
                    item.put("month", month);
                    item.put("blockName", block);
                    item.put("resourceType", type);
                    item.put("totalQuantity", total);
                    item.put("unit", unitMap.getOrDefault(type, ""));
                    result.add(item);
                }
            }
        }

        return ResponseEntity.ok(result);
    }
}
