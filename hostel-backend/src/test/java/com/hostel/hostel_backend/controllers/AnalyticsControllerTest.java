package com.hostel.hostel_backend.controllers;

import com.hostel.hostel_backend.models.Complaint;
import com.hostel.hostel_backend.models.Fee;
import com.hostel.hostel_backend.models.Leave;
import com.hostel.hostel_backend.models.Room;
import com.hostel.hostel_backend.models.User;
import com.hostel.hostel_backend.repositories.*;
import com.hostel.hostel_backend.security.JwtAuthFilter;
import com.hostel.hostel_backend.security.JwtTokenProvider;
import com.hostel.hostel_backend.security.RateLimitFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AnalyticsController.class)
@AutoConfigureMockMvc(addFilters = false)
public class AnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private RoomRepository roomRepository;

    @MockBean
    private ComplaintRepository complaintRepository;

    @MockBean
    private LeaveRepository leaveRepository;

    @MockBean
    private FeeRepository feeRepository;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @MockBean
    private RateLimitFilter rateLimitFilter;

    private User adminUser;

    @BeforeEach
    public void setUp() {
        adminUser = new User();
        adminUser.setId("admin123");
        adminUser.setEmail("admin@test.com");
        adminUser.setRole("SUPER_ADMIN");

        // Mock authentication context for helper methods
        Authentication auth = Mockito.mock(Authentication.class);
        when(auth.getName()).thenReturn("admin@test.com");
        SecurityContext sc = Mockito.mock(SecurityContext.class);
        when(sc.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(sc);

        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));
    }

    @Test
    public void testGetOccupancySuccess() throws Exception {
        Room r1 = new Room();
        r1.setId("room1");
        r1.setBlockName("A");
        r1.setCapacity(4);
        r1.setOccupantIds(Arrays.asList("s1", "s2"));

        Room r2 = new Room();
        r2.setId("room2");
        r2.setBlockName("B");
        r2.setCapacity(2);
        r2.setOccupantIds(Collections.singletonList("s3"));

        when(roomRepository.findAll()).thenReturn(Arrays.asList(r1, r2));

        mockMvc.perform(get("/api/analytics/occupancy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.blockName == 'A')].filled").value(2))
                .andExpect(jsonPath("$[?(@.blockName == 'A')].available").value(2))
                .andExpect(jsonPath("$[?(@.blockName == 'A')].totalCapacity").value(4))
                .andExpect(jsonPath("$[?(@.blockName == 'B')].filled").value(1))
                .andExpect(jsonPath("$[?(@.blockName == 'B')].available").value(1));
    }

    @Test
    public void testGetComplaintHeatmapSuccess() throws Exception {
        Room r1 = new Room();
        r1.setBlockName("A");
        r1.setOccupantIds(Arrays.asList("stud1", "stud2"));
        when(roomRepository.findAll()).thenReturn(Collections.singletonList(r1));

        Complaint c1 = new Complaint();
        c1.setId("c1");
        c1.setStudentId("stud1");
        c1.setCategory("PLUMBING");
        c1.setRaisedAt(LocalDateTime.now().minusDays(10));

        Complaint c2 = new Complaint();
        c2.setId("c2");
        c2.setStudentId("stud2");
        c2.setCategory("ELECTRICAL");
        c2.setRaisedAt(LocalDateTime.now().minusDays(5));

        when(complaintRepository.findByRaisedAtAfter(any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(c1, c2));

        mockMvc.perform(get("/api/analytics/complaint-heatmap"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.blockName == 'A' && @.category == 'PLUMBING')].count").value(1))
                .andExpect(jsonPath("$[?(@.blockName == 'A' && @.category == 'ELECTRICAL')].count").value(1));
    }

    @Test
    public void testGetLeavePatternsSuccess() throws Exception {
        Leave l1 = new Leave();
        l1.setId("leave1");
        l1.setLeaveType("CASUAL");
        // Applied on a fixed day: 2026-07-08 (Wednesday)
        l1.setAppliedAt(LocalDateTime.of(2026, 7, 8, 10, 0));

        Leave l2 = new Leave();
        l2.setId("leave2");
        l2.setLeaveType("MEDICAL");
        // Applied on a fixed day: 2026-07-09 (Thursday)
        l2.setAppliedAt(LocalDateTime.of(2026, 7, 9, 11, 0));

        when(leaveRepository.findAll()).thenReturn(Arrays.asList(l1, l2));

        mockMvc.perform(get("/api/analytics/leave-patterns"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.byLeaveType.CASUAL").value(1))
                .andExpect(jsonPath("$.byLeaveType.MEDICAL").value(1))
                .andExpect(jsonPath("$.byDayOfWeek.WEDNESDAY").value(1))
                .andExpect(jsonPath("$.byDayOfWeek.THURSDAY").value(1));
    }

    @Test
    public void testGetFeeForecastSuccess() throws Exception {
        // Mock a pending fee in 10 days
        Fee pendingFee = new Fee();
        pendingFee.setId("f1");
        pendingFee.setStudentId("stud1");
        pendingFee.setAmount(1000.0);
        pendingFee.setStatus("PENDING");
        pendingFee.setDueDate(LocalDate.now().plusDays(10).toString());

        // Mock historical paid fees (one on time, one late)
        Fee historical1 = new Fee();
        historical1.setId("f2");
        historical1.setStatus("PAID");
        historical1.setDueDate("2026-06-01");
        historical1.setPaidDate("2026-06-01"); // on time

        Fee historical2 = new Fee();
        historical2.setId("f3");
        historical2.setStatus("PAID");
        historical2.setDueDate("2026-06-01");
        historical2.setPaidDate("2026-06-15"); // late

        when(feeRepository.findByStatus("PENDING")).thenReturn(Collections.singletonList(pendingFee));
        when(feeRepository.findAll()).thenReturn(Arrays.asList(pendingFee, historical1, historical2));

        // Total scored historical: 2 (historical1 and historical2)
        // On time historical: 1
        // Rate: 1 / 2 = 50% (0.5)
        // Pending next 30 days: 1000.0
        // Projected: 1000.0 * 0.5 = 500.0
        mockMvc.perform(get("/api/analytics/fee-forecast"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendingAmountNext30Days").value(1000.0))
                .andExpect(jsonPath("$.historicalOnTimeRate").value(0.5))
                .andExpect(jsonPath("$.projectedCollection").value(500.0));
    }

    @Test
    public void testAccessDeniedForStudent() throws Exception {
        User studentUser = new User();
        studentUser.setId("stud123");
        studentUser.setEmail("student@test.com");
        studentUser.setRole("STUDENT");

        Authentication auth = Mockito.mock(Authentication.class);
        when(auth.getName()).thenReturn("student@test.com");
        SecurityContext sc = Mockito.mock(SecurityContext.class);
        when(sc.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(sc);

        when(userRepository.findByEmail("student@test.com")).thenReturn(Optional.of(studentUser));

        mockMvc.perform(get("/api/analytics/occupancy"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").isNotEmpty());
    }

    @Test
    public void testGetOccupancyEmpty() throws Exception {
        when(roomRepository.findAll()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/analytics/occupancy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    public void testGetFeeForecastDivisionByZeroCheck() throws Exception {
        // No pending fees, no historical fees
        when(feeRepository.findByStatus("PENDING")).thenReturn(Collections.emptyList());
        when(feeRepository.findAll()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/analytics/fee-forecast"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendingAmountNext30Days").value(0.0))
                .andExpect(jsonPath("$.historicalOnTimeRate").value(1.0))
                .andExpect(jsonPath("$.projectedCollection").value(0.0));
    }
}
