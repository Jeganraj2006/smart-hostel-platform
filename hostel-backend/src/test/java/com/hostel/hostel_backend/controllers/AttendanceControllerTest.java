package com.hostel.hostel_backend.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hostel.hostel_backend.models.Attendance;
import com.hostel.hostel_backend.models.Leave;
import com.hostel.hostel_backend.models.User;
import com.hostel.hostel_backend.repositories.AttendanceRepository;
import com.hostel.hostel_backend.repositories.LeaveRepository;
import com.hostel.hostel_backend.repositories.RoomRepository;
import com.hostel.hostel_backend.repositories.UserRepository;
import com.hostel.hostel_backend.security.JwtAuthFilter;
import com.hostel.hostel_backend.security.JwtTokenProvider;
import com.hostel.hostel_backend.security.RateLimitFilter;
import com.hostel.hostel_backend.services.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AttendanceController.class)
@AutoConfigureMockMvc(addFilters = false)
public class AttendanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AttendanceRepository attendanceRepository;

    @MockBean
    private LeaveRepository leaveRepository;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private RoomRepository roomRepository;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @MockBean
    private RateLimitFilter rateLimitFilter;

    @MockBean
    private AuditService auditService;

    private User guardUser;
    private Leave approvedLeave;

    @BeforeEach
    public void setup() {
        guardUser = new User();
        guardUser.setId("guard-123");
        guardUser.setName("Guard Bob");
        guardUser.setRole("SECURITY_GUARD");
        guardUser.setEmail("bob@security.com");

        approvedLeave = new Leave();
        approvedLeave.setId("leave-123");
        approvedLeave.setStudentId("student-123");
        approvedLeave.setStatus("APPROVED");
        approvedLeave.setFromDate(LocalDate.now().minusDays(1).toString());
        approvedLeave.setToDate(LocalDate.now().plusDays(1).toString());

        when(userRepository.findByEmail("bob@security.com")).thenReturn(Optional.of(guardUser));
    }

    @Test
    @WithMockUser(username = "bob@security.com", roles = {"SECURITY_GUARD"})
    public void testExitScanSuccessful() throws Exception {
        when(leaveRepository.findById("leave-123")).thenReturn(Optional.of(approvedLeave));
        when(attendanceRepository.findByLeaveId("leave-123")).thenReturn(Optional.empty());
        when(attendanceRepository.save(any(Attendance.class))).thenAnswer(i -> {
            Attendance a = i.getArgument(0);
            a.setId("att-123");
            return a;
        });

        Map<String, String> request = new HashMap<>();
        request.put("leaveId", "leave-123");

        mockMvc.perform(post("/api/gate/scan")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OUT"))
                .andExpect(jsonPath("$.leaveId").value("leave-123"));
    }

    @Test
    @WithMockUser(username = "bob@security.com", roles = {"SECURITY_GUARD"})
    public void testEntryScanSuccessful() throws Exception {
        Attendance attendance = new Attendance();
        attendance.setId("att-123");
        attendance.setLeaveId("leave-123");
        attendance.setStudentId("student-123");
        attendance.setStatus("OUT");
        attendance.setExitScannedAt(LocalDateTime.now().minusHours(1));

        when(leaveRepository.findById("leave-123")).thenReturn(Optional.of(approvedLeave));
        when(attendanceRepository.findByLeaveId("leave-123")).thenReturn(Optional.of(attendance));
        when(attendanceRepository.save(any(Attendance.class))).thenAnswer(i -> i.getArgument(0));

        Map<String, String> request = new HashMap<>();
        request.put("leaveId", "leave-123");

        mockMvc.perform(post("/api/gate/scan")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RETURNED"))
                .andExpect(jsonPath("$.entryScannedAt").exists());
    }

    @Test
    @WithMockUser(username = "bob@security.com", roles = {"SECURITY_GUARD"})
    public void testScanRejectedWhenLeaveNotApproved() throws Exception {
        approvedLeave.setStatus("PENDING");
        when(leaveRepository.findById("leave-123")).thenReturn(Optional.of(approvedLeave));

        Map<String, String> request = new HashMap<>();
        request.put("leaveId", "leave-123");

        mockMvc.perform(post("/api/gate/scan")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("only allowed for APPROVED leaves")));
    }

    @Test
    @WithMockUser(username = "bob@security.com", roles = {"SECURITY_GUARD"})
    public void testScanRejectedWhenOutsideLeaveWindow() throws Exception {
        approvedLeave.setFromDate(LocalDate.now().plusDays(1).toString());
        approvedLeave.setToDate(LocalDate.now().plusDays(2).toString());
        when(leaveRepository.findById("leave-123")).thenReturn(Optional.of(approvedLeave));

        Map<String, String> request = new HashMap<>();
        request.put("leaveId", "leave-123");

        mockMvc.perform(post("/api/gate/scan")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("outside the approved leave window")));
    }
}
