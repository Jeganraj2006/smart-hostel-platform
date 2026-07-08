package com.hostel.hostel_backend.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hostel.hostel_backend.dto.RegisterRequest;
import com.hostel.hostel_backend.models.Room;
import com.hostel.hostel_backend.models.User;
import com.hostel.hostel_backend.repositories.RoomRepository;
import com.hostel.hostel_backend.repositories.UserRepository;
import com.hostel.hostel_backend.services.AuthService;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StudentController.class)
@AutoConfigureMockMvc(addFilters = false)
public class StudentAnonymizationTest {

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

    private User adminUser;
    private User studentUser;

    @BeforeEach
    public void setUp() {
        adminUser = new User();
        adminUser.setId("admin123");
        adminUser.setEmail("admin@test.com");
        adminUser.setRole("SUPER_ADMIN");

        studentUser = new User();
        studentUser.setId("stud456");
        studentUser.setName("John Doe");
        studentUser.setEmail("john@test.com");
        studentUser.setPhone("1234567890");
        studentUser.setRole("STUDENT");
        studentUser.setAccountStatus("ACTIVE");
    }

    @Test
    public void testAnonymizeStudentSuccessByAdmin() throws Exception {
        Authentication auth = Mockito.mock(Authentication.class);
        when(auth.getName()).thenReturn("admin@test.com");
        SecurityContext sc = Mockito.mock(SecurityContext.class);
        when(sc.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(sc);

        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));
        when(userRepository.findById("stud456")).thenReturn(Optional.of(studentUser));

        // Mock room allocation removal
        Room room = new Room();
        room.setId("room1");
        room.setBlockName("A");
        room.setOccupantIds(new ArrayList<>(Arrays.asList("stud456", "other")));
        when(roomRepository.findByOccupantIdsContaining("stud456")).thenReturn(Optional.of(room));

        mockMvc.perform(delete("/api/students/stud456/data"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Student data successfully anonymized to preserve audit integrity under DPDP Act 2023 compliance."));

        // Verify PII is anonymized
        verify(userRepository).save(studentUser);
        assert "Anonymized Student".equals(studentUser.getName());
        assert "0000000000".equals(studentUser.getPhone());
        assert "anonymized_stud456@hostel.internal".equals(studentUser.getEmail());
        assert "ANONYMIZED".equals(studentUser.getAccountStatus());
        assert !studentUser.isActive();

        // Verify occupant removed from room
        assert !room.getOccupantIds().contains("stud456");
        verify(roomRepository).save(room);
    }

    @Test
    public void testAnonymizeStudentAccessDeniedForNonAdmin() throws Exception {
        User callerStudent = new User();
        callerStudent.setId("stud456");
        callerStudent.setEmail("john@test.com");
        callerStudent.setRole("STUDENT");

        Authentication auth = Mockito.mock(Authentication.class);
        when(auth.getName()).thenReturn("john@test.com");
        SecurityContext sc = Mockito.mock(SecurityContext.class);
        when(sc.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(sc);

        when(userRepository.findByEmail("john@test.com")).thenReturn(Optional.of(callerStudent));
        when(userRepository.findById("stud456")).thenReturn(Optional.of(studentUser));

        mockMvc.perform(delete("/api/students/stud456/data"))
                .andExpect(status().isForbidden());
    }
}
