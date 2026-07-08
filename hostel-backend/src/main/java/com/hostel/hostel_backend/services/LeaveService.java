package com.hostel.hostel_backend.services;

import com.hostel.hostel_backend.dto.LeaveRequest;
import com.hostel.hostel_backend.models.Leave;
import com.hostel.hostel_backend.models.User;
import com.hostel.hostel_backend.repositories.LeaveRepository;
import com.hostel.hostel_backend.repositories.UserRepository;
import com.hostel.hostel_backend.exceptions.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LeaveService {

    @Autowired
    private LeaveRepository leaveRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditService auditService;

    @Autowired
    private QrCodeService qrCodeService;

    @Autowired
    private NotificationService notificationService;

    // Get current logged-in user
    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    // Get approval chain based on leave type
    private List<Leave.ApprovalStep> buildChain(String leaveType) {
        List<Leave.ApprovalStep> steps = new ArrayList<>();

        if (leaveType.equals("CASUAL")) {
            steps.add(createStep("WARDEN"));
            steps.add(createStep("HOD"));
            steps.add(createStep("PARENT"));
        } else if (leaveType.equals("EMERGENCY")) {
            steps.add(createStep("WARDEN"));
        } else {
            // OUTPASS, HOLIDAY, MEDICAL
            steps.add(createStep("WARDEN"));
            steps.add(createStep("PARENT"));
        }

        if (!steps.isEmpty()) steps.get(0).setCurrent(true);
        return steps;
    }

    private Leave.ApprovalStep createStep(String role) {
        Leave.ApprovalStep step = new Leave.ApprovalStep();
        step.setRole(role);
        return step;
    }

    // Student applies leave
    public Leave applyLeave(LeaveRequest request) {
        User student = getCurrentUser();

        Leave leave = new Leave();
        leave.setStudentId(student.getId());
        leave.setStudentName(student.getName());
        leave.setStudentEmail(student.getEmail());
        leave.setLeaveType(request.getLeaveType());
        leave.setFromDate(request.getFromDate());
        leave.setToDate(request.getToDate());
        leave.setReturnTime(request.getReturnTime());
        leave.setReason(request.getReason());
        leave.setDestination(request.getDestination());
        leave.setStatus("PENDING");
        leave.setApprovalSteps(buildChain(request.getLeaveType()));
        leave.setCurrentLevel(0);

        return leaveRepository.save(leave);
    }

    // Get student's own leaves
    public List<Leave> getMyLeaves() {
        User student = getCurrentUser();
        return leaveRepository.findByStudentIdOrderByAppliedAtDesc(student.getId());
    }

    // Get all pending leaves (for warden/HOD)
    public List<Leave> getPendingLeaves() {
        return leaveRepository.findByStatus("PENDING");
    }

    // Approve leave at current level
    public Leave approveLeave(String leaveId, int level) {
        User approver = getCurrentUser();

        Leave leave = leaveRepository.findById(leaveId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave not found"));

        List<Leave.ApprovalStep> steps = leave.getApprovalSteps();
        Leave.ApprovalStep currentStep = steps.get(leave.getCurrentLevel());

        currentStep.setApproved(true);
        currentStep.setCurrent(false);
        currentStep.setApproverName(approver.getName());
        currentStep.setApproverId(approver.getId());
        currentStep.setActionAt(LocalDateTime.now());

        int nextLevel = leave.getCurrentLevel() + 1;

        if (nextLevel >= steps.size()) {
            // All levels approved
            leave.setStatus("APPROVED");
            try {
                String qr = qrCodeService.generateQrBase64(leave.getId(), leave.getToDate());
                leave.setQrCode(qr);
            } catch (Exception e) {
                // Log and proceed
            }
        } else {
            // Move to next level
            leave.setCurrentLevel(nextLevel);
            steps.get(nextLevel).setCurrent(true);
        }

        leave.setUpdatedAt(LocalDateTime.now());
        Leave savedLeave = leaveRepository.save(leave);

        if ("APPROVED".equals(savedLeave.getStatus())) {
            notificationService.sendAlert(
                savedLeave.getStudentEmail(),
                String.format("Dear %s,\n\nYour leave application (ID: %s) from %s to %s has been APPROVED.\n\nBest regards,\nHostel Management System",
                    savedLeave.getStudentName(), savedLeave.getId(), savedLeave.getFromDate(), savedLeave.getToDate())
            );
        }

        // Audit Logging
        Map<String, String> metadata = new HashMap<>();
        metadata.put("level", String.valueOf(level));
        metadata.put("status", savedLeave.getStatus());
        auditService.log(
            approver.getId(),
            approver.getRole(),
            "APPROVE",
            "LEAVE",
            leaveId,
            metadata
        );

        return savedLeave;
    }

    // Reject leave
    public Leave rejectLeave(String leaveId, String reason) {
        Leave leave = leaveRepository.findById(leaveId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave not found"));

        leave.setStatus("REJECTED");
        leave.setRejectionReason(reason);
        leave.setUpdatedAt(LocalDateTime.now());
        Leave savedLeave = leaveRepository.save(leave);

        notificationService.sendAlert(
            savedLeave.getStudentEmail(),
            String.format("Dear %s,\n\nYour leave application (ID: %s) from %s to %s has been REJECTED.\nReason: %s\n\nBest regards,\nHostel Management System",
                savedLeave.getStudentName(), savedLeave.getId(), savedLeave.getFromDate(), savedLeave.getToDate(), reason)
        );

        // Audit Logging
        User approver = getCurrentUser();
        Map<String, String> metadata = new HashMap<>();
        metadata.put("reason", reason);
        auditService.log(
            approver.getId(),
            approver.getRole(),
            "REJECT",
            "LEAVE",
            leaveId,
            metadata
        );

        return savedLeave;
    }

    // Emergency override by warden
    public Leave emergencyOverride(String leaveId, String reason) {
        User warden = getCurrentUser();

        Leave leave = leaveRepository.findById(leaveId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave not found"));

        leave.setStatus("APPROVED");
        leave.setEmergencyOverride(true);
        leave.setEmergencyReason(reason);
        leave.setOverriddenBy(warden.getName());
        leave.setUpdatedAt(LocalDateTime.now());

        // Mark all steps as approved
        leave.getApprovalSteps().forEach(s -> s.setApproved(true));

        try {
            String qr = qrCodeService.generateQrBase64(leave.getId(), leave.getToDate());
            leave.setQrCode(qr);
        } catch (Exception e) {
            // Log and proceed
        }

        Leave savedLeave = leaveRepository.save(leave);

        notificationService.sendAlert(
            savedLeave.getStudentEmail(),
            String.format("Dear %s,\n\nYour leave application (ID: %s) from %s to %s has been APPROVED via emergency override by warden %s.\nReason: %s\n\nBest regards,\nHostel Management System",
                savedLeave.getStudentName(), savedLeave.getId(), savedLeave.getFromDate(), savedLeave.getToDate(), warden.getName(), reason)
        );

        // Audit Logging
        Map<String, String> metadata = new HashMap<>();
        metadata.put("reason", reason);
        auditService.log(
            warden.getId(),
            warden.getRole(),
            "EMERGENCY_OVERRIDE",
            "LEAVE",
            leaveId,
            metadata
        );

        return savedLeave;
    }

    // Send reminder
    public void sendReminder(String leaveId) {
        User current = getCurrentUser();
        Leave leave = leaveRepository.findById(leaveId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave not found"));

        if (!current.getId().equals(leave.getStudentId())) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied: You do not own this leave request.");
        }

        leave.setReminderCount(leave.getReminderCount() + 1);
        leaveRepository.save(leave);
    }
}