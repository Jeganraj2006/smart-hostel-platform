package com.hostel.hostel_backend.models;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Document(collection = "leaves")
public class Leave {

    @Id
    private String id;

    private String studentId;
    private String studentName;
    private String studentEmail;
    private String roomNo;

    private String leaveType; // OUTPASS, CASUAL, HOLIDAY, MEDICAL, EMERGENCY
    private String fromDate;
    private String toDate;
    private String returnTime;
    private String reason;
    private String destination;
    private String documentUrl;

    private String status; // PENDING, APPROVED, REJECTED
    private String rejectionReason;

    private List<ApprovalStep> approvalSteps;
    private int currentLevel = 0;
    private int reminderCount = 0;

    private boolean emergencyOverride = false;
    private String emergencyReason;
    private String overriddenBy;

    private String qrCode;

    private LocalDateTime appliedAt = LocalDateTime.now();
    private LocalDateTime updatedAt;

    @Data
    public static class ApprovalStep {
        private String role;
        private String approverId;
        private String approverName;
        private boolean approved = false;
        private boolean current = false;
        private String comment;
        private LocalDateTime actionAt;
    }
}