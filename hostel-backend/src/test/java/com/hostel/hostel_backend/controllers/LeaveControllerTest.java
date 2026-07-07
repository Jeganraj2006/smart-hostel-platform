package com.hostel.hostel_backend.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hostel.hostel_backend.dto.LeaveRequest;
import com.hostel.hostel_backend.models.Leave;
import com.hostel.hostel_backend.services.LeaveService;
import com.hostel.hostel_backend.security.JwtAuthFilter;
import com.hostel.hostel_backend.security.JwtTokenProvider;
import com.hostel.hostel_backend.security.RateLimitFilter;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LeaveController.class)
@AutoConfigureMockMvc(addFilters = false)
public class LeaveControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LeaveService leaveService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @MockBean
    private RateLimitFilter rateLimitFilter;

    @Test
    public void testApplyLeaveSuccess() throws Exception {
        LeaveRequest request = new LeaveRequest();
        request.setLeaveType("CASUAL");
        request.setReason("Visiting home");

        Leave leave = new Leave();
        leave.setId("leave123");
        leave.setLeaveType("CASUAL");
        leave.setReason("Visiting home");
        leave.setStatus("PENDING");

        Mockito.when(leaveService.applyLeave(any(LeaveRequest.class)))
                .thenReturn(leave);

        mockMvc.perform(post("/api/leaves/apply")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("leave123"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    public void testApproveLeaveSuccess() throws Exception {
        Map<String, Integer> body = new HashMap<>();
        body.put("level", 1);

        Leave leave = new Leave();
        leave.setId("leave123");
        leave.setStatus("APPROVED");

        Mockito.when(leaveService.approveLeave(anyString(), anyInt()))
                .thenReturn(leave);

        mockMvc.perform(put("/api/leaves/leave123/approve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    public void testRejectLeaveSuccess() throws Exception {
        Map<String, String> body = new HashMap<>();
        body.put("reason", "Incomplete documents");

        Leave leave = new Leave();
        leave.setId("leave123");
        leave.setStatus("REJECTED");

        Mockito.when(leaveService.rejectLeave(anyString(), anyString()))
                .thenReturn(leave);

        mockMvc.perform(put("/api/leaves/leave123/reject")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }
}
