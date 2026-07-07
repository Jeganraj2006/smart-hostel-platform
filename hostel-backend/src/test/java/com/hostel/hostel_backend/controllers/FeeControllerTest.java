package com.hostel.hostel_backend.controllers;

import com.hostel.hostel_backend.models.Fee;
import com.hostel.hostel_backend.models.User;
import com.hostel.hostel_backend.repositories.FeeRepository;
import com.hostel.hostel_backend.repositories.UserRepository;
import com.hostel.hostel_backend.security.JwtAuthFilter;
import com.hostel.hostel_backend.security.JwtTokenProvider;
import com.hostel.hostel_backend.security.RateLimitFilter;
import com.hostel.hostel_backend.services.AuditService;
import com.hostel.hostel_backend.services.FeeRiskService;
import com.hostel.hostel_backend.services.ReceiptService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FeeController.class)
@AutoConfigureMockMvc(addFilters = false)
public class FeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private FeeRepository feeRepository;
    @MockBean private UserRepository userRepository;
    @MockBean private AuditService auditService;
    @MockBean private ReceiptService receiptService;
    @MockBean private FeeRiskService feeRiskService;
    @MockBean private JwtAuthFilter jwtAuthFilter;
    @MockBean private JwtTokenProvider jwtTokenProvider;
    @MockBean private RateLimitFilter rateLimitFilter;

    private User student;
    private Fee pendingFee;

    @BeforeEach
    void setup() {
        student = new User();
        student.setId("student-001");
        student.setEmail("alice@hostel.com");
        student.setRole("STUDENT");

        pendingFee = new Fee();
        pendingFee.setId("fee-001");
        pendingFee.setStudentId("student-001");
        pendingFee.setFeeType("HOSTEL");
        pendingFee.setAmount(5000.0);
        pendingFee.setStatus("PENDING");
        pendingFee.setDueDate("2026-08-01");
    }

    // ─── Happy path ───────────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "alice@hostel.com", roles = "STUDENT")
    void testPayOwnFeeSucceeds() throws Exception {
        when(userRepository.findByEmail("alice@hostel.com")).thenReturn(Optional.of(student));
        when(feeRepository.findById("fee-001")).thenReturn(Optional.of(pendingFee));
        when(feeRepository.save(any(Fee.class))).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(auditService).log(anyString(), anyString(), anyString(), anyString(), anyString(), anyMap());

        mockMvc.perform(post("/api/fees/fee-001/pay")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.paidDate").isNotEmpty());

        // Verify audit was called with correct action
        verify(auditService, times(1)).log(
                eq("student-001"), eq("STUDENT"), eq("FEE_PAID"), eq("FEE"), eq("fee-001"), anyMap()
        );
    }

    // ─── Ownership enforcement ────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "alice@hostel.com", roles = "STUDENT")
    void testStudentCannotPayAnotherStudentsFee() throws Exception {
        // The fee belongs to a different student
        pendingFee.setStudentId("student-999");

        when(userRepository.findByEmail("alice@hostel.com")).thenReturn(Optional.of(student));
        when(feeRepository.findById("fee-001")).thenReturn(Optional.of(pendingFee));

        mockMvc.perform(post("/api/fees/fee-001/pay")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Access denied: you can only pay your own fee."));

        // No audit should be recorded for a rejected action
        verify(auditService, never()).log(anyString(), anyString(), anyString(), anyString(), anyString(), anyMap());
        verify(feeRepository, never()).save(any());
    }

    // ─── Double-payment guard ─────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "alice@hostel.com", roles = "STUDENT")
    void testCannotPayAlreadyPaidFee() throws Exception {
        pendingFee.setStatus("PAID");
        pendingFee.setPaidDate("2026-07-01");

        when(userRepository.findByEmail("alice@hostel.com")).thenReturn(Optional.of(student));
        when(feeRepository.findById("fee-001")).thenReturn(Optional.of(pendingFee));

        mockMvc.perform(post("/api/fees/fee-001/pay")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Fee is already marked as PAID."));

        verify(feeRepository, never()).save(any());
    }

    // ─── Fee not found ────────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "alice@hostel.com", roles = "STUDENT")
    void testPayNonExistentFeeReturns404() throws Exception {
        when(userRepository.findByEmail("alice@hostel.com")).thenReturn(Optional.of(student));
        when(feeRepository.findById("missing-fee")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/fees/missing-fee/pay")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    // ─── WARDEN bypass ───────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "warden@hostel.com", roles = "WARDEN")
    void testWardenCanPayAnyFeeWithoutOwnershipCheck() throws Exception {
        User warden = new User();
        warden.setId("warden-001");
        warden.setEmail("warden@hostel.com");
        warden.setRole("WARDEN");

        // Fee belongs to a different student — warden should still succeed
        pendingFee.setStudentId("student-999");

        when(userRepository.findByEmail("warden@hostel.com")).thenReturn(Optional.of(warden));
        when(feeRepository.findById("fee-001")).thenReturn(Optional.of(pendingFee));
        when(feeRepository.save(any(Fee.class))).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(auditService).log(anyString(), anyString(), anyString(), anyString(), anyString(), anyMap());

        mockMvc.perform(post("/api/fees/fee-001/pay")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));
    }
}
