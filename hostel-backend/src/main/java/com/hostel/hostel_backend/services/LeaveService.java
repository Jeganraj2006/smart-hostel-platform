package com.hostel.hostel_backend.services;

import com.hostel.hostel_backend.dto.LeaveRequest;
import com.hostel.hostel_backend.models.Leave;
import com.hostel.hostel_backend.models.User;
import com.hostel.hostel_backend.repositories.LeaveRepository;
import com.hostel.hostel_backend.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class LeaveService {

    @Autowired
    private LeaveRepository leaveRepository;

    @Autowired
    private UserRepository userRepository;

    // Get current logged-in user
    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
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
                .orElseThrow(() -> new RuntimeException("Leave not found"));

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
        } else {
            // Move to next level
            leave.setCurrentLevel(nextLevel);
            steps.get(nextLevel).setCurrent(true);
        }

        leave.setUpdatedAt(LocalDateTime.now());
        return leaveRepository.save(leave);
    }

    // Reject leave
    public Leave rejectLeave(String leaveId, String reason) {
        Leave leave = leaveRepository.findById(leaveId)
                .orElseThrow(() -> new RuntimeException("Leave not found"));

        leave.setStatus("REJECTED");
        leave.setRejectionReason(reason);
        leave.setUpdatedAt(LocalDateTime.now());
        return leaveRepository.save(leave);
    }

    // Emergency override by warden
    public Leave emergencyOverride(String leaveId, String reason) {
        User warden = getCurrentUser();

        Leave leave = leaveRepository.findById(leaveId)
                .orElseThrow(() -> new RuntimeException("Leave not found"));

        leave.setStatus("APPROVED");
        leave.setEmergencyOverride(true);
        leave.setEmergencyReason(reason);
        leave.setOverriddenBy(warden.getName());
        leave.setUpdatedAt(LocalDateTime.now());

        // Mark all steps as approved
        leave.getApprovalSteps().forEach(s -> s.setApproved(true));

        return leaveRepository.save(leave);
    }

    // Send reminder
    public void sendReminder(String leaveId) {
        Leave leave = leaveRepository.findById(leaveId)
                .orElseThrow(() -> new RuntimeException("Leave not found"));

        leave.setReminderCount(leave.getReminderCount() + 1);
        leaveRepository.save(leave);
        // TODO: Send notification to current approver
    }
}