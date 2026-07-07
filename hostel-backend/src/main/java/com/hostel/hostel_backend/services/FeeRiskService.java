package com.hostel.hostel_backend.services;

import com.hostel.hostel_backend.models.Fee;
import com.hostel.hostel_backend.models.User;
import com.hostel.hostel_backend.repositories.FeeRepository;
import com.hostel.hostel_backend.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * FeeRiskService — Explainable fee-payment risk scoring.
 *
 * Scoring model (per student, over all recorded fees):
 *
 *   Each fee contributes to one of three counters:
 *     • overdue       — status is OVERDUE
 *     • paidLate      — status is PAID but paidDate > dueDate
 *     • paidOnTime    — status is PAID and paidDate <= dueDate
 *   (PENDING fees are counted but do not affect the tier unless they push
 *    the overdue ratio above the HIGH threshold.)
 *
 *   Raw risk points = (overdue × 3) + (paidLate × 1)
 *   Total scored    = overdue + paidLate + paidOnTime  (excludes open PENDING)
 *
 *   Tier thresholds (evaluated in order):
 *     HIGH   — overdue >= 2  OR  riskPoints / totalScored >= 0.5 (if totalScored > 0)
 *     MEDIUM — overdue == 1  OR  riskPoints / totalScored >= 0.2
 *     LOW    — everything else
 */
@Service
public class FeeRiskService {

    @Autowired
    private FeeRepository feeRepository;

    @Autowired
    private UserRepository userRepository;

    // ─────────────────────────────────────────────────────────────────────────
    // Public result DTO
    // ─────────────────────────────────────────────────────────────────────────

    public static class RiskResult {
        private final String studentId;
        private final String studentName;
        private final String email;
        private final String tier;          // LOW / MEDIUM / HIGH
        private final int    riskPoints;
        private final String reason;        // human-readable explanation
        private final int    overdueCount;
        private final int    paidLateCount;
        private final int    paidOnTimeCount;
        private final int    totalFees;

        public RiskResult(String studentId, String studentName, String email,
                          String tier, int riskPoints, String reason,
                          int overdueCount, int paidLateCount,
                          int paidOnTimeCount, int totalFees) {
            this.studentId      = studentId;
            this.studentName    = studentName;
            this.email          = email;
            this.tier           = tier;
            this.riskPoints     = riskPoints;
            this.reason         = reason;
            this.overdueCount   = overdueCount;
            this.paidLateCount  = paidLateCount;
            this.paidOnTimeCount = paidOnTimeCount;
            this.totalFees      = totalFees;
        }

        public String getStudentId()       { return studentId; }
        public String getStudentName()     { return studentName; }
        public String getEmail()           { return email; }
        public String getTier()            { return tier; }
        public int    getRiskPoints()      { return riskPoints; }
        public String getReason()          { return reason; }
        public int    getOverdueCount()    { return overdueCount; }
        public int    getPaidLateCount()   { return paidLateCount; }
        public int    getPaidOnTimeCount() { return paidOnTimeCount; }
        public int    getTotalFees()       { return totalFees; }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Core scoring
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Compute a risk score for a single student.
     *
     * @param studentId  MongoDB user ID of the student
     * @return RiskResult with tier and human-readable reason
     */
    public RiskResult computeRiskScore(String studentId) {
        List<Fee> fees = feeRepository.findByStudentId(studentId);

        int overdueCount    = 0;
        int paidLateCount   = 0;
        int paidOnTimeCount = 0;

        for (Fee fee : fees) {
            String status = fee.getStatus();
            if ("OVERDUE".equals(status)) {
                overdueCount++;
            } else if ("PAID".equals(status)) {
                if (isPaidLate(fee)) {
                    paidLateCount++;
                } else {
                    paidOnTimeCount++;
                }
            }
            // PENDING fees are not scored (no outcome yet)
        }

        int totalScored = overdueCount + paidLateCount + paidOnTimeCount;
        int riskPoints  = (overdueCount * 3) + (paidLateCount * 1);
        double ratio    = totalScored > 0 ? (double) riskPoints / totalScored : 0.0;

        String tier;
        String reason;

        // Tier evaluation:
        //   Overdue count is the primary signal and locks the tier when present.
        //   Ratio-based escalation only activates when there are zero overdue fees
        //   (i.e., the student only has late-paid fees, no confirmed overdue ones).
        if (overdueCount >= 2) {
            // Two or more overdue fees → always HIGH
            tier = "HIGH";
        } else if (overdueCount == 1) {
            // Exactly one overdue fee → always MEDIUM
            tier = "MEDIUM";
        } else if (totalScored > 0 && ratio >= 0.5) {
            // No overdue, but more than half of scored fees were paid late → HIGH
            tier = "HIGH";
        } else if (totalScored > 0 && ratio >= 0.2) {
            // No overdue, but 20-49% of scored fees paid late → MEDIUM
            tier = "MEDIUM";
        } else {
            tier = "LOW";
        }

        reason = (totalScored == 0 && tier.equals("LOW"))
                ? "No completed fee history found."
                : buildReason(overdueCount, paidLateCount, paidOnTimeCount, totalScored);

        // Resolve student name/email for the DTO (best-effort; missing → "Unknown")
        String name  = "Unknown";
        String email = "—";
        Optional<User> userOpt = userRepository.findById(studentId);
        if (userOpt.isPresent()) {
            name  = userOpt.get().getName()  != null ? userOpt.get().getName()  : name;
            email = userOpt.get().getEmail() != null ? userOpt.get().getEmail() : email;
        }

        return new RiskResult(studentId, name, email, tier, riskPoints, reason,
                overdueCount, paidLateCount, paidOnTimeCount, fees.size());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Bulk endpoint helper — all MEDIUM/HIGH students, highest-risk first
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns risk assessments for every student who has at least one fee record,
     * filtered to MEDIUM or HIGH tiers, sorted highest-risk first (by riskPoints desc).
     */
    public List<RiskResult> getAllAtRiskStudents() {
        // Collect distinct studentIds from all fee records
        List<Fee> allFees = feeRepository.findAll();
        Set<String> studentIds = allFees.stream()
                .map(Fee::getStudentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        return studentIds.stream()
                .map(this::computeRiskScore)
                .filter(r -> "MEDIUM".equals(r.getTier()) || "HIGH".equals(r.getTier()))
                .sorted(Comparator.comparingInt(RiskResult::getRiskPoints).reversed())
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns true if the fee was paid after its due date.
     * Gracefully handles missing / malformed date strings by returning false.
     */
    private boolean isPaidLate(Fee fee) {
        if (fee.getPaidDate() == null || fee.getDueDate() == null) return false;
        try {
            LocalDate paid = LocalDate.parse(fee.getPaidDate());
            LocalDate due  = LocalDate.parse(fee.getDueDate());
            return paid.isAfter(due);
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    /**
     * Produces a human-readable explanation of the risk score, e.g.:
     *   "2 overdue, 1 late payment out of 4 recorded fees."
     *   "0 overdue, 2 of last 3 fees paid late."
     */
    private String buildReason(int overdue, int late, int onTime, int total) {
        List<String> parts = new ArrayList<>();

        if (overdue > 0) {
            parts.add(overdue + (overdue == 1 ? " fee is" : " fees are") + " overdue");
        }
        if (late > 0) {
            parts.add(late + " of last " + total + (late == 1 ? " fee paid" : " fees paid") + " late");
        }
        if (overdue == 0 && late == 0) {
            parts.add(onTime + " of " + total + " fees paid on time");
        }

        return String.join("; ", parts) + ".";
    }
}
