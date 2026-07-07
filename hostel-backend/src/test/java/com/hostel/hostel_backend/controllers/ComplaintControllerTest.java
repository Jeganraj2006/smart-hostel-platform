package com.hostel.hostel_backend.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hostel.hostel_backend.models.Complaint;
import com.hostel.hostel_backend.models.User;
import com.hostel.hostel_backend.repositories.ComplaintRepository;
import com.hostel.hostel_backend.repositories.UserRepository;
import com.hostel.hostel_backend.security.JwtAuthFilter;
import com.hostel.hostel_backend.security.JwtTokenProvider;
import com.hostel.hostel_backend.security.RateLimitFilter;
import com.hostel.hostel_backend.services.AuditService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ComplaintController.class)
@AutoConfigureMockMvc(addFilters = false)
public class ComplaintControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ComplaintRepository complaintRepository;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @MockBean
    private RateLimitFilter rateLimitFilter;

    @MockBean
    private AuditService auditService;

    @Test
    @WithMockUser(username = "student@example.com")
    public void testCreateComplaintSuccess() throws Exception {
        User user = new User();
        user.setId("user123");
        user.setEmail("student@example.com");
        user.setName("Student Name");

        Complaint request = new Complaint();
        request.setCategory("ELECTRICITY");
        request.setPriority("MEDIUM");
        request.setDescription("Light bulb not working");

        Complaint savedComplaint = new Complaint();
        savedComplaint.setId("complaint123");
        savedComplaint.setStudentId("user123");
        savedComplaint.setStudentName("Student Name");
        savedComplaint.setCategory("ELECTRICITY");
        savedComplaint.setPriority("MEDIUM");
        savedComplaint.setDescription("Light bulb not working");
        savedComplaint.setStatus("OPEN");

        Mockito.when(userRepository.findByEmail("student@example.com"))
                .thenReturn(Optional.of(user));
        Mockito.when(complaintRepository.save(any(Complaint.class)))
                .thenReturn(savedComplaint);

        mockMvc.perform(post("/api/complaints")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("complaint123"))
                .andExpect(jsonPath("$.studentId").value("user123"))
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    @WithMockUser(username = "warden@example.com")
    public void testUpdateStatusSuccess() throws Exception {
        Complaint complaint = new Complaint();
        complaint.setId("complaint123");
        complaint.setStudentId("user123");
        complaint.setStatus("OPEN");

        Map<String, String> body = new HashMap<>();
        body.put("status", "RESOLVED");

        Complaint updatedComplaint = new Complaint();
        updatedComplaint.setId("complaint123");
        updatedComplaint.setStudentId("user123");
        updatedComplaint.setStatus("RESOLVED");

        User warden = new User();
        warden.setId("warden123");
        warden.setEmail("warden@example.com");
        warden.setName("Warden Name");
        warden.setRole("WARDEN");

        Mockito.when(userRepository.findByEmail("warden@example.com"))
                .thenReturn(Optional.of(warden));
        Mockito.when(complaintRepository.findById("complaint123"))
                .thenReturn(Optional.of(complaint));
        Mockito.when(complaintRepository.save(any(Complaint.class)))
                .thenReturn(updatedComplaint);

        mockMvc.perform(put("/api/complaints/complaint123/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));
    }
}
