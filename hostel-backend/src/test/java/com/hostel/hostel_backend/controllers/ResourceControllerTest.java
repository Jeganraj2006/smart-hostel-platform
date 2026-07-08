package com.hostel.hostel_backend.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hostel.hostel_backend.models.ResourceLog;
import com.hostel.hostel_backend.models.User;
import com.hostel.hostel_backend.repositories.ResourceLogRepository;
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

import java.util.Arrays;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ResourceController.class)
@AutoConfigureMockMvc(addFilters = false)
public class ResourceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ResourceLogRepository resourceLogRepository;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @MockBean
    private RateLimitFilter rateLimitFilter;

    private User staffUser;
    private User adminUser;

    @BeforeEach
    public void setUp() {
        staffUser = new User();
        staffUser.setId("staff123");
        staffUser.setEmail("staff@test.com");
        staffUser.setRole("STAFF");

        adminUser = new User();
        adminUser.setId("admin123");
        adminUser.setEmail("admin@test.com");
        adminUser.setRole("SUPER_ADMIN");
    }

    @Test
    public void testLogResourceSuccessByStaff() throws Exception {
        Authentication auth = Mockito.mock(Authentication.class);
        when(auth.getName()).thenReturn("staff@test.com");
        SecurityContext sc = Mockito.mock(SecurityContext.class);
        when(sc.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(sc);

        when(userRepository.findByEmail("staff@test.com")).thenReturn(Optional.of(staffUser));

        ResourceLog reqLog = new ResourceLog();
        reqLog.setDate("2026-07-08");
        reqLog.setResourceType("ELECTRICITY");
        reqLog.setBlockName("A");
        reqLog.setQuantity(340.5);
        reqLog.setUnit("kWh");

        ResourceLog savedLog = new ResourceLog();
        savedLog.setId("log123");
        savedLog.setDate("2026-07-08");
        savedLog.setResourceType("ELECTRICITY");
        savedLog.setBlockName("A");
        savedLog.setQuantity(340.5);
        savedLog.setUnit("kWh");
        savedLog.setRecordedBy("staff@test.com");

        when(resourceLogRepository.save(any(ResourceLog.class))).thenReturn(savedLog);

        mockMvc.perform(post("/api/resources")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(reqLog)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("log123"))
                .andExpect(jsonPath("$.recordedBy").value("staff@test.com"));
    }

    @Test
    public void testLogResourceForbiddenForStudent() throws Exception {
        User studentUser = new User();
        studentUser.setId("student123");
        studentUser.setEmail("student@test.com");
        studentUser.setRole("STUDENT");

        Authentication auth = Mockito.mock(Authentication.class);
        when(auth.getName()).thenReturn("student@test.com");
        SecurityContext sc = Mockito.mock(SecurityContext.class);
        when(sc.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(sc);

        when(userRepository.findByEmail("student@test.com")).thenReturn(Optional.of(studentUser));

        ResourceLog reqLog = new ResourceLog();
        reqLog.setDate("2026-07-08");
        reqLog.setResourceType("WATER");
        reqLog.setBlockName("B");
        reqLog.setQuantity(2000.0);
        reqLog.setUnit("L");

        mockMvc.perform(post("/api/resources")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(reqLog)))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testGetSummarySuccessByAdmin() throws Exception {
        Authentication auth = Mockito.mock(Authentication.class);
        when(auth.getName()).thenReturn("admin@test.com");
        SecurityContext sc = Mockito.mock(SecurityContext.class);
        when(sc.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(sc);

        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));

        ResourceLog l1 = new ResourceLog();
        l1.setDate("2026-07-01");
        l1.setResourceType("ELECTRICITY");
        l1.setBlockName("A");
        l1.setQuantity(100.0);
        l1.setUnit("kWh");

        ResourceLog l2 = new ResourceLog();
        l2.setDate("2026-07-02");
        l2.setResourceType("ELECTRICITY");
        l2.setBlockName("A");
        l2.setQuantity(150.0);
        l2.setUnit("kWh");

        ResourceLog l3 = new ResourceLog();
        l3.setDate("2026-07-03");
        l3.setResourceType("WATER");
        l3.setBlockName("B");
        l3.setQuantity(1000.0);
        l3.setUnit("L");

        when(resourceLogRepository.findAll()).thenReturn(Arrays.asList(l1, l2, l3));

        mockMvc.perform(get("/api/resources/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.blockName == 'A' && @.resourceType == 'ELECTRICITY')].totalQuantity").value(250.0))
                .andExpect(jsonPath("$[?(@.blockName == 'B' && @.resourceType == 'WATER')].totalQuantity").value(1000.0));
    }

    @Test
    public void testLogResourceBadRequest() throws Exception {
        Authentication auth = Mockito.mock(Authentication.class);
        when(auth.getName()).thenReturn("staff@test.com");
        SecurityContext sc = Mockito.mock(SecurityContext.class);
        when(sc.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(sc);

        when(userRepository.findByEmail("staff@test.com")).thenReturn(Optional.of(staffUser));

        // Invalid resourceType
        ResourceLog reqLog1 = new ResourceLog();
        reqLog1.setDate("2026-07-08");
        reqLog1.setResourceType("COAL"); // invalid!
        reqLog1.setBlockName("A");
        reqLog1.setQuantity(50.0);
        reqLog1.setUnit("kg");

        mockMvc.perform(post("/api/resources")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(reqLog1)))
                .andExpect(status().isBadRequest());

        // Negative quantity
        ResourceLog reqLog2 = new ResourceLog();
        reqLog2.setDate("2026-07-08");
        reqLog2.setResourceType("WATER");
        reqLog2.setBlockName("A");
        reqLog2.setQuantity(-10.0); // invalid!
        reqLog2.setUnit("L");

        mockMvc.perform(post("/api/resources")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(reqLog2)))
                .andExpect(status().isBadRequest());
    }
}
