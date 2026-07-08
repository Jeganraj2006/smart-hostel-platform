package com.hostel.hostel_backend.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hostel.hostel_backend.models.Room;
import com.hostel.hostel_backend.models.User;
import com.hostel.hostel_backend.repositories.RoomRepository;
import com.hostel.hostel_backend.repositories.UserRepository;
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

import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ParentController.class)
@AutoConfigureMockMvc(addFilters = false)
public class ParentControllerTest {

    @Autowired
    private MockMvc mockMvc;

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
    private com.hostel.hostel_backend.repositories.LeaveRepository leaveRepository;

    @MockBean
    private com.hostel.hostel_backend.repositories.FeeRepository feeRepository;

    @MockBean
    private com.hostel.hostel_backend.repositories.ComplaintRepository complaintRepository;

    @MockBean
    private com.hostel.hostel_backend.repositories.AttendanceRepository attendanceRepository;

    private User parentUser;
    private User studentUser;
    private Room studentRoom;

    @BeforeEach
    public void setUp() {
        parentUser = new User();
        parentUser.setId("parent123");
        parentUser.setEmail("parent@test.com");
        parentUser.setRole("PARENT");
        parentUser.setLinkedStudentId("student123");
        parentUser.setAccountStatus("ACTIVE");
        parentUser.setActive(true);

        studentUser = new User();
        studentUser.setId("student123");
        studentUser.setEmail("child@test.com");
        studentUser.setName("Child Name");
        studentUser.setPhone("9876543210");
        studentUser.setRole("STUDENT");
        studentUser.setAccountStatus("ACTIVE");
        studentUser.setActive(true);

        studentRoom = new Room();
        studentRoom.setId("roomA101");
        studentRoom.setRoomNumber("101");
        studentRoom.setBlockName("A");
        studentRoom.setRoomType("SHARED");

        // Mock security context
        Authentication authentication = Mockito.mock(Authentication.class);
        when(authentication.getName()).thenReturn("parent@test.com");
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findByEmail("parent@test.com")).thenReturn(Optional.of(parentUser));
    }

    @Test
    public void testGetLinkedStudentSuccessWithRoom() throws Exception {
        when(userRepository.findById("student123")).thenReturn(Optional.of(studentUser));
        when(roomRepository.findByOccupantIdsContaining("student123")).thenReturn(Optional.of(studentRoom));

        mockMvc.perform(get("/api/parent/my-student"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("student123"))
                .andExpect(jsonPath("$.name").value("Child Name"))
                .andExpect(jsonPath("$.email").value("child@test.com"))
                .andExpect(jsonPath("$.roomNumber").value("101"))
                .andExpect(jsonPath("$.blockName").value("A"))
                .andExpect(jsonPath("$.roomType").value("SHARED"));
    }

    @Test
    public void testGetLinkedStudentSuccessWithoutRoom() throws Exception {
        when(userRepository.findById("student123")).thenReturn(Optional.of(studentUser));
        when(roomRepository.findByOccupantIdsContaining("student123")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/parent/my-student"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("student123"))
                .andExpect(jsonPath("$.roomNumber").isEmpty())
                .andExpect(jsonPath("$.blockName").isEmpty());
    }

    @Test
    public void testGetLinkedStudentNotFound() throws Exception {
        when(userRepository.findById("student123")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/parent/my-student"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testGetLinkedStudentNoLinkage() throws Exception {
        parentUser.setLinkedStudentId(null);

        mockMvc.perform(get("/api/parent/my-student"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testGetLinkedStudentAccessDeniedForStudent() throws Exception {
        User studentCaller = new User();
        studentCaller.setId("student_caller");
        studentCaller.setEmail("student_caller@test.com");
        studentCaller.setRole("STUDENT");

        Authentication auth = Mockito.mock(Authentication.class);
        when(auth.getName()).thenReturn("student_caller@test.com");
        SecurityContext sc = Mockito.mock(SecurityContext.class);
        when(sc.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(sc);

        when(userRepository.findByEmail("student_caller@test.com")).thenReturn(Optional.of(studentCaller));

        mockMvc.perform(get("/api/parent/my-student"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").isNotEmpty());
    }

    @Test
    public void testGetParentTimelineSuccess() throws Exception {
        // Mock a Leave
        com.hostel.hostel_backend.models.Leave leave = new com.hostel.hostel_backend.models.Leave();
        leave.setId("leave1");
        leave.setStudentId("student123");
        leave.setLeaveType("CASUAL");
        leave.setDestination("Home");
        leave.setStatus("APPROVED");
        leave.setAppliedAt(java.time.LocalDateTime.of(2026, 7, 8, 14, 0));
        when(leaveRepository.findByStudentIdOrderByAppliedAtDesc("student123"))
                .thenReturn(java.util.Collections.singletonList(leave));

        // Mock a Fee
        com.hostel.hostel_backend.models.Fee fee = new com.hostel.hostel_backend.models.Fee();
        fee.setId("fee1");
        fee.setStudentId("student123");
        fee.setFeeType("HOSTEL");
        fee.setAmount(5000.0);
        fee.setStatus("PAID");
        fee.setPaidDate("2026-07-08"); // parses to 2026-07-08T00:00:00
        when(feeRepository.findByStudentId("student123"))
                .thenReturn(java.util.Collections.singletonList(fee));

        // Mock a Complaint
        com.hostel.hostel_backend.models.Complaint complaint = new com.hostel.hostel_backend.models.Complaint();
        complaint.setId("comp1");
        complaint.setStudentId("student123");
        complaint.setCategory("PLUMBING");
        complaint.setDescription("Leak in washroom");
        complaint.setStatus("OPEN");
        complaint.setRaisedAt(java.time.LocalDateTime.of(2026, 7, 8, 15, 0));
        when(complaintRepository.findByStudentId("student123"))
                .thenReturn(java.util.Collections.singletonList(complaint));

        // Mock an Attendance (Gate Activity)
        com.hostel.hostel_backend.models.Attendance att = new com.hostel.hostel_backend.models.Attendance();
        att.setId("att1");
        att.setStudentId("student123");
        att.setExitScannedAt(java.time.LocalDateTime.of(2026, 7, 8, 16, 0));
        att.setEntryScannedAt(java.time.LocalDateTime.of(2026, 7, 8, 18, 0));
        att.setStatus("RETURNED");
        when(attendanceRepository.findByStudentId("student123"))
                .thenReturn(java.util.Collections.singletonList(att));

        // Perform request
        // Order of expected timestamps (descending):
        // 1. GATE_ENTRY (18:00)
        // 2. GATE_EXIT (16:00)
        // 3. COMPLAINT (15:00)
        // 4. LEAVE (14:00)
        // 5. FEE (00:00)
        mockMvc.perform(get("/api/parent/timeline"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("GATE_ENTRY"))
                .andExpect(jsonPath("$[0].timestamp").value("2026-07-08T18:00"))
                .andExpect(jsonPath("$[1].type").value("GATE_EXIT"))
                .andExpect(jsonPath("$[1].timestamp").value("2026-07-08T16:00"))
                .andExpect(jsonPath("$[2].type").value("COMPLAINT"))
                .andExpect(jsonPath("$[2].timestamp").value("2026-07-08T15:00"))
                .andExpect(jsonPath("$[3].type").value("LEAVE"))
                .andExpect(jsonPath("$[3].timestamp").value("2026-07-08T14:00"))
                .andExpect(jsonPath("$[4].type").value("FEE"))
                .andExpect(jsonPath("$[4].timestamp").value("2026-07-08T00:00"));
    }

    @Test
    public void testGetTimelineNoLinkage() throws Exception {
        parentUser.setLinkedStudentId(null);

        mockMvc.perform(get("/api/parent/timeline"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testGetTimelineAccessDeniedForStudent() throws Exception {
        User studentCaller = new User();
        studentCaller.setId("student_caller");
        studentCaller.setEmail("student_caller@test.com");
        studentCaller.setRole("STUDENT");

        Authentication auth = Mockito.mock(Authentication.class);
        when(auth.getName()).thenReturn("student_caller@test.com");
        SecurityContext sc = Mockito.mock(SecurityContext.class);
        when(sc.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(sc);

        when(userRepository.findByEmail("student_caller@test.com")).thenReturn(Optional.of(studentCaller));

        mockMvc.perform(get("/api/parent/timeline"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").isNotEmpty());
    }

    @Test
    public void testGetTimelineAccessDeniedForWarden() throws Exception {
        User wardenCaller = new User();
        wardenCaller.setId("warden_caller");
        wardenCaller.setEmail("warden_caller@test.com");
        wardenCaller.setRole("WARDEN");

        Authentication auth = Mockito.mock(Authentication.class);
        when(auth.getName()).thenReturn("warden_caller@test.com");
        SecurityContext sc = Mockito.mock(SecurityContext.class);
        when(sc.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(sc);

        when(userRepository.findByEmail("warden_caller@test.com")).thenReturn(Optional.of(wardenCaller));

        mockMvc.perform(get("/api/parent/timeline"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").isNotEmpty());
    }

    @Test
    public void testGetParentTimelineEmpty() throws Exception {
        when(leaveRepository.findByStudentIdOrderByAppliedAtDesc("student123"))
                .thenReturn(java.util.Collections.emptyList());
        when(feeRepository.findByStudentId("student123"))
                .thenReturn(java.util.Collections.emptyList());
        when(complaintRepository.findByStudentId("student123"))
                .thenReturn(java.util.Collections.emptyList());
        when(attendanceRepository.findByStudentId("student123"))
                .thenReturn(java.util.Collections.emptyList());

        mockMvc.perform(get("/api/parent/timeline"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }
}
