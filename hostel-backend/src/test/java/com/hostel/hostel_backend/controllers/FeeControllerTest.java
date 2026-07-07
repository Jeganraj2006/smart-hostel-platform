package com.hostel.hostel_backend.controllers;

import com.hostel.hostel_backend.models.Fee;
import com.hostel.hostel_backend.models.User;
import com.hostel.hostel_backend.repositories.FeeRepository;
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

import java.util.List;
import java.util.Optional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FeeController.class)
@AutoConfigureMockMvc(addFilters = false)
public class FeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FeeRepository feeRepository;

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
    public void testGetMyFeesSuccess() throws Exception {
        User user = new User();
        user.setId("user123");
        user.setEmail("student@example.com");

        Fee fee = new Fee();
        fee.setId("fee123");
        fee.setStudentId("user123");
        fee.setFeeType("HOSTEL");
        fee.setAmount(5000.0);
        fee.setStatus("PENDING");

        Mockito.when(userRepository.findByEmail("student@example.com"))
                .thenReturn(Optional.of(user));
        Mockito.when(feeRepository.findByStudentId("user123"))
                .thenReturn(List.of(fee));

        mockMvc.perform(get("/api/fees/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("fee123"))
                .andExpect(jsonPath("$[0].studentId").value("user123"))
                .andExpect(jsonPath("$[0].feeType").value("HOSTEL"));
    }

    @Test
    public void testGetAllFeesSuccess() throws Exception {
        Fee fee = new Fee();
        fee.setId("fee123");
        fee.setStudentId("user123");
        fee.setFeeType("HOSTEL");

        Mockito.when(feeRepository.findAll())
                .thenReturn(List.of(fee));

        mockMvc.perform(get("/api/fees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("fee123"));
    }
}
