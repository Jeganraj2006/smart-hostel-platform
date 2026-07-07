package com.hostel.hostel_backend.services;

import com.hostel.hostel_backend.models.PreventiveFlag;
import com.hostel.hostel_backend.repositories.ComplaintRepository;
import com.hostel.hostel_backend.repositories.PreventiveFlagRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class PreventiveMaintenanceService {

    @Autowired
    private ComplaintRepository complaintRepository;

    @Autowired
    private PreventiveFlagRepository preventiveFlagRepository;

    @Autowired
    private AuditService auditService;

    /**
     * Checks if the count of complaints for a specific assetId and category
     * in the last 60 days is >= 3. If so, creates a logged audit trail and
     * a PreventiveFlag record.
     */
    public void checkRecurrence(String assetId, String category) {
        if (assetId == null || assetId.trim().isEmpty() || category == null || category.trim().isEmpty()) {
            return;
        }

        LocalDateTime sixtyDaysAgo = LocalDateTime.now().minusDays(60);
        long count = complaintRepository.countByAssetIdAndCategoryAndRaisedAtAfter(assetId, category, sixtyDaysAgo);

        log.info("Recurrence check for assetId: {}, category: {}. Count in last 60 days: {}", assetId, category, count);

        if (count >= 3) {
            log.warn("Recurrence threshold met (>= 3) for assetId: {}, category: {}. Flagging for preventive maintenance.", assetId, category);

            // Re-use or update existing active (unresolved) flag if present
            Optional<PreventiveFlag> existingOpt = preventiveFlagRepository
                    .findByAssetIdAndCategoryAndResolved(assetId, category, false);

            PreventiveFlag flag;
            if (existingOpt.isPresent()) {
                flag = existingOpt.get();
                flag.setComplaintCount(count);
            } else {
                flag = new PreventiveFlag();
                flag.setAssetId(assetId);
                flag.setCategory(category);
                flag.setComplaintCount(count);
            }

            PreventiveFlag saved = preventiveFlagRepository.save(flag);

            // Log audit log with action "PREVENTIVE_MAINTENANCE_FLAG"
            Map<String, String> metadata = new HashMap<>();
            metadata.put("assetId", assetId);
            metadata.put("category", category);
            metadata.put("complaintCount", String.valueOf(count));

            auditService.log(
                "SYSTEM",
                "SYSTEM",
                "PREVENTIVE_MAINTENANCE_FLAG",
                "PREVENTIVE_FLAG",
                saved.getId(),
                metadata
            );
        }
    }
}
