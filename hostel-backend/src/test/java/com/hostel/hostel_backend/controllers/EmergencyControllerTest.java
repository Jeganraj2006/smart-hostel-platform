package com.hostel.hostel_backend.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hostel.hostel_backend.models.Room;
import com.hostel.hostel_backend.models.User;
import com.hostel.hostel_backend.repositories.RoomRepository;
import com.hostel.hostel_backend.repositories.UserRepository;
import com.hostel.hostel_backend.services.AuditService;
import com.hostel.hostel_backend.services.NotificationService;
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EmergencyController.class)
@AutoConfigureMockMvc(addFilters = false)
public class EmergencyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private RoomRepository roomRepository;

    @MockBean
    private NotificationService notificationService;

    @MockBean
    private AuditService auditService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @MockBean
    private RateLimitFilter rateLimitFilter;

    private User wardenUser;
    private User studentUser1;
    private User studentUser2;
    private User pendingStudent;

    @BeforeEach
    public void setUp() {
        wardenUser = new User();
        wardenUser.setId("warden1");
        wardenUser.setEmail("warden@hostel.com");
        wardenUser.setRole("WARDEN");
        wardenUser.setAccountStatus("ACTIVE");
        wardenUser.setActive(true);

        studentUser1 = new User();
        studentUser1.setId("student1");
        studentUser1.setEmail("student1@hostel.com");
        studentUser1.setRole("STUDENT");
        studentUser1.setAccountStatus("ACTIVE");
        studentUser1.setActive(true);

        studentUser2 = new User();
        studentUser2.setId("student2");
        studentUser2.setEmail("student2@hostel.com");
        studentUser2.setRole("STUDENT");
        studentUser2.setAccountStatus("ACTIVE");
        studentUser2.setActive(true);

        pendingStudent = new User();
        pendingStudent.setId("student3");
        pendingStudent.setEmail("pending@hostel.com");
        pendingStudent.setRole("STUDENT");
        pendingStudent.setAccountStatus("PENDING");
        pendingStudent.setActive(true);

        // Mock Security Context
        Authentication authentication = Mockito.mock(Authentication.class);
        when(authentication.getName()).thenReturn("warden@hostel.com");
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findByEmail("warden@hostel.com")).thenReturn(Optional.of(wardenUser));
    }

    @Test
    public void testBroadcastForbiddenForStudent() throws Exception {
        // Change authenticated user to student
        User currentStudent = new User();
        currentStudent.setId("student_req");
        currentStudent.setEmail("student@hostel.com");
        currentStudent.setRole("STUDENT");
        currentStudent.setAccountStatus("ACTIVE");
        currentStudent.setActive(true);

        Authentication auth = Mockito.mock(Authentication.class);
        when(auth.getName()).thenReturn("student@hostel.com");
        SecurityContext sc = Mockito.mock(SecurityContext.class);
        when(sc.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(sc);

        when(userRepository.findByEmail("student@hostel.com")).thenReturn(Optional.of(currentStudent));

        EmergencyController.BroadcastRequest request = new EmergencyController.BroadcastRequest();
        request.setScope("HOSTEL");
        request.setMessage("Test message");

        mockMvc.perform(post("/api/emergency/broadcast")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testBroadcastHostelSuccess() throws Exception {
        when(userRepository.findByRole("STUDENT")).thenReturn(Arrays.asList(studentUser1, studentUser2, pendingStudent));

        EmergencyController.BroadcastRequest request = new EmergencyController.BroadcastRequest();
        request.setScope("HOSTEL");
        request.setMessage("Water supply maintenance today");

        mockMvc.perform(post("/api/emergency/broadcast")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recipientCount").value(2))
                .andExpect(jsonPath("$.message").value("Broadcast sent successfully"));

        // Verify notification service was called for active students only
        verify(notificationService, times(1)).sendAlert("student1@hostel.com", "Water supply maintenance today");
        verify(notificationService, times(1)).sendAlert("student2@hostel.com", "Water supply maintenance today");
        verify(notificationService, never()).sendAlert("pending@hostel.com", "Water supply maintenance today");

        // Verify audit service log
        verify(auditService, times(1)).log(
                eq("warden1"),
                eq("WARDEN"),
                eq("EMERGENCY_BROADCAST"),
                eq("EMERGENCY"),
                isNull(),
                any()
        );
    }

    @Test
    public void testBroadcastBlockSuccess() throws Exception {
        when(userRepository.findByRole("STUDENT")).thenReturn(Arrays.asList(studentUser1, studentUser2));

        Room roomA = new Room();
        roomA.setId("room1");
        roomA.setBlockName("A");
        roomA.setOccupantIds(Collections.singletonList("student1"));

        when(roomRepository.findByBlockName("A")).thenReturn(Collections.singletonList(roomA));

        EmergencyController.BroadcastRequest request = new EmergencyController.BroadcastRequest();
        request.setScope("BLOCK");
        request.setBlockName("A");
        request.setMessage("Block A cleanup");

        mockMvc.perform(post("/api/emergency/broadcast")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recipientCount").value(1));

        verify(notificationService, times(1)).sendAlert("student1@hostel.com", "Block A cleanup");
        verify(notificationService, never()).sendAlert(eq("student2@hostel.com"), anyString());
    }

    @Test
    public void testBroadcastBadRequestMissingMessage() throws Exception {
        EmergencyController.BroadcastRequest request = new EmergencyController.BroadcastRequest();
        request.setScope("HOSTEL");

        mockMvc.perform(post("/api/emergency/broadcast")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testBroadcastBadRequestMissingBlockName() throws Exception {
        EmergencyController.BroadcastRequest request = new EmergencyController.BroadcastRequest();
        request.setScope("BLOCK");
        request.setMessage("Alert");

        mockMvc.perform(post("/api/emergency/broadcast")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testBroadcastBadRequestInvalidScope() throws Exception {
        EmergencyController.BroadcastRequest request = new EmergencyController.BroadcastRequest();
        request.setScope("FLOOR");
        request.setMessage("Alert");

        mockMvc.perform(post("/api/emergency/broadcast")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
