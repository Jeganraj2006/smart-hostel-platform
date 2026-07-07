package com.hostel.hostel_backend.services;

import com.hostel.hostel_backend.models.Fee;
import com.hostel.hostel_backend.models.User;
import com.hostel.hostel_backend.repositories.FeeRepository;
import com.hostel.hostel_backend.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class FeeRiskServiceTest {

    @Mock private FeeRepository feeRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private FeeRiskService feeRiskService;

    private User student;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        student = new User();
        student.setId("student-001");
        student.setName("Alice");
        student.setEmail("alice@hostel.com");
    }

    // ── Helper factories ──────────────────────────────────────────────────────

    private Fee makeFee(String status, String dueDate, String paidDate) {
        Fee f = new Fee();
        f.setId("fee-" + Math.random());
        f.setStudentId("student-001");
        f.setStatus(status);
        f.setDueDate(dueDate);
        f.setPaidDate(paidDate);
        f.setAmount(5000.0);
        f.setFeeType("HOSTEL");
        return f;
    }

    private void stubStudent() {
        when(userRepository.findById("student-001")).thenReturn(Optional.of(student));
    }

    // ── LOW risk ─────────────────────────────────────────────────────────────

    @Test
    void testAllFeePaidOnTime_LowRisk() {
        stubStudent();
        List<Fee> fees = Arrays.asList(
                makeFee("PAID", "2026-05-01", "2026-04-28"),
                makeFee("PAID", "2026-06-01", "2026-05-30"),
                makeFee("PAID", "2026-07-01", "2026-06-29")
        );
        when(feeRepository.findByStudentId("student-001")).thenReturn(fees);

        FeeRiskService.RiskResult result = feeRiskService.computeRiskScore("student-001");

        assertEquals("LOW",     result.getTier());
        assertEquals(0,         result.getOverdueCount());
        assertEquals(0,         result.getPaidLateCount());
        assertEquals(3,         result.getPaidOnTimeCount());
        assertEquals(0,         result.getRiskPoints());
        assertTrue(result.getReason().contains("on time"),
                "Reason should mention on-time payments, was: " + result.getReason());
    }

    @Test
    void testNoFeeHistory_LowRisk() {
        stubStudent();
        when(feeRepository.findByStudentId("student-001")).thenReturn(Collections.emptyList());

        FeeRiskService.RiskResult result = feeRiskService.computeRiskScore("student-001");

        assertEquals("LOW", result.getTier());
        assertEquals(0, result.getRiskPoints());
        assertTrue(result.getReason().contains("No completed"));
    }

    // ── MEDIUM risk ───────────────────────────────────────────────────────────

    @Test
    void testOneOverdue_MediumRisk() {
        stubStudent();
        List<Fee> fees = Arrays.asList(
                makeFee("OVERDUE", "2026-04-01", null),
                makeFee("PAID",    "2026-05-01", "2026-04-29"),
                makeFee("PAID",    "2026-06-01", "2026-05-30")
        );
        when(feeRepository.findByStudentId("student-001")).thenReturn(fees);

        FeeRiskService.RiskResult result = feeRiskService.computeRiskScore("student-001");

        assertEquals("MEDIUM", result.getTier());
        assertEquals(1,        result.getOverdueCount());
        assertEquals(3,        result.getRiskPoints());   // 1 overdue × 3
        assertTrue(result.getReason().contains("overdue"));
    }

    @Test
    void testTwoLatePaidOfThree_MediumRisk() {
        stubStudent();
        // 2 late + 1 on-time → riskPoints=2, totalScored=3, ratio≈0.67? No, 2/3=0.67 → HIGH
        // Need exactly ratio between 0.2–0.5 for MEDIUM: 1 late out of 5 → ratio=0.2
        List<Fee> fees = Arrays.asList(
                makeFee("PAID", "2026-01-01", "2026-01-10"),  // late
                makeFee("PAID", "2026-02-01", "2026-01-29"),  // on time
                makeFee("PAID", "2026-03-01", "2026-02-27"),  // on time
                makeFee("PAID", "2026-04-01", "2026-03-29"),  // on time
                makeFee("PAID", "2026-05-01", "2026-04-28")   // on time
        );
        when(feeRepository.findByStudentId("student-001")).thenReturn(fees);

        FeeRiskService.RiskResult result = feeRiskService.computeRiskScore("student-001");

        // riskPoints=1, totalScored=5, ratio=0.2 → MEDIUM (border)
        assertEquals("MEDIUM", result.getTier());
        assertEquals(1,        result.getPaidLateCount());
        assertEquals(1,        result.getRiskPoints());
        assertTrue(result.getReason().contains("late"));
    }

    // ── HIGH risk ─────────────────────────────────────────────────────────────

    @Test
    void testTwoOrMoreOverdue_HighRisk() {
        stubStudent();
        List<Fee> fees = Arrays.asList(
                makeFee("OVERDUE", "2026-02-01", null),
                makeFee("OVERDUE", "2026-03-01", null),
                makeFee("PAID",    "2026-04-01", "2026-04-05")   // late too
        );
        when(feeRepository.findByStudentId("student-001")).thenReturn(fees);

        FeeRiskService.RiskResult result = feeRiskService.computeRiskScore("student-001");

        assertEquals("HIGH", result.getTier());
        assertEquals(2,      result.getOverdueCount());
        assertEquals(7,      result.getRiskPoints());  // 2×3 + 1×1
        assertTrue(result.getReason().contains("overdue"));
    }

    @Test
    void testHighLatioWithNoOverdue_HighRisk() {
        stubStudent();
        // 3 late out of 4 → ratio = 3/4 = 0.75 ≥ 0.5 → HIGH
        List<Fee> fees = Arrays.asList(
                makeFee("PAID", "2026-01-01", "2026-01-15"),  // late
                makeFee("PAID", "2026-02-01", "2026-02-10"),  // late
                makeFee("PAID", "2026-03-01", "2026-03-12"),  // late
                makeFee("PAID", "2026-04-01", "2026-03-28")   // on time
        );
        when(feeRepository.findByStudentId("student-001")).thenReturn(fees);

        FeeRiskService.RiskResult result = feeRiskService.computeRiskScore("student-001");

        assertEquals("HIGH", result.getTier());
        assertEquals(3,      result.getPaidLateCount());
        assertEquals(3,      result.getRiskPoints());  // 3 late × 1
        assertTrue(result.getReason().contains("late"));
    }

    // ── Sorting test ──────────────────────────────────────────────────────────

    @Test
    void testGetAllAtRiskStudents_SortedHighestFirst() {
        // student-001: 2 overdue (HIGH, 6 pts)
        // student-002: 1 overdue (MEDIUM, 3 pts)
        Fee s1f1 = makeFee("OVERDUE", "2026-01-01", null);
        s1f1.setStudentId("student-001");
        Fee s1f2 = makeFee("OVERDUE", "2026-02-01", null);
        s1f2.setStudentId("student-001");

        Fee s2f1 = makeFee("OVERDUE", "2026-01-01", null);
        s2f1.setStudentId("student-002");
        Fee s2f2 = makeFee("PAID",    "2026-02-01", "2026-01-29");
        s2f2.setStudentId("student-002");

        when(feeRepository.findAll()).thenReturn(Arrays.asList(s1f1, s1f2, s2f1, s2f2));
        when(feeRepository.findByStudentId("student-001")).thenReturn(Arrays.asList(s1f1, s1f2));
        when(feeRepository.findByStudentId("student-002")).thenReturn(Arrays.asList(s2f1, s2f2));

        User s2 = new User();
        s2.setId("student-002");
        s2.setName("Bob");
        s2.setEmail("bob@hostel.com");

        when(userRepository.findById("student-001")).thenReturn(Optional.of(student));
        when(userRepository.findById("student-002")).thenReturn(Optional.of(s2));

        List<FeeRiskService.RiskResult> results = feeRiskService.getAllAtRiskStudents();

        assertEquals(2,         results.size());
        assertEquals("HIGH",   results.get(0).getTier());
        assertEquals("MEDIUM", results.get(1).getTier());
        assertTrue(results.get(0).getRiskPoints() >= results.get(1).getRiskPoints(),
                "Results should be sorted highest-risk first");
    }
}
