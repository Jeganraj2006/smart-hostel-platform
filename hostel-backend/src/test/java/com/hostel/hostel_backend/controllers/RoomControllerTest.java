package com.hostel.hostel_backend.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hostel.hostel_backend.models.Room;
import com.hostel.hostel_backend.models.User;
import com.hostel.hostel_backend.repositories.RoomRepository;
import com.hostel.hostel_backend.repositories.UserRepository;
import com.hostel.hostel_backend.security.JwtAuthFilter;
import com.hostel.hostel_backend.security.JwtTokenProvider;
import com.hostel.hostel_backend.security.RateLimitFilter;
import com.hostel.hostel_backend.services.AuditService;
import com.hostel.hostel_backend.services.RoomAllocationService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RoomController.class)
@AutoConfigureMockMvc(addFilters = false)
public class RoomControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RoomRepository roomRepository;

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

    @MockBean
    private RoomAllocationService roomAllocationService;

    @Test
    @WithMockUser(username = "admin@example.com")
    public void testCreateRoomSuccess() throws Exception {
        Room room = new Room();
        room.setId("room123");
        room.setRoomNumber("101");
        room.setBlockName("Block A");
        room.setCapacity(2);

        User admin = new User();
        admin.setId("admin123");
        admin.setEmail("admin@example.com");
        admin.setRole("ADMIN");

        Mockito.when(userRepository.findByEmail("admin@example.com"))
                .thenReturn(Optional.of(admin));
        Mockito.when(roomRepository.save(any(Room.class)))
                .thenReturn(room);

        mockMvc.perform(post("/api/rooms")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(room)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("room123"))
                .andExpect(jsonPath("$.roomNumber").value("101"));
    }

    @Test
    @WithMockUser(username = "admin@example.com")
    public void testAssignStudentSuccess() throws Exception {
        Room room = new Room();
        room.setId("room123");
        room.setCapacity(2);
        room.setRoomNumber("101");
        room.setStatus("AVAILABLE");

        User student = new User();
        student.setId("student123");
        student.setEmail("student@example.com");
        student.setName("Student Name");

        User admin = new User();
        admin.setId("admin123");
        admin.setEmail("admin@example.com");
        admin.setRole("ADMIN");

        Mockito.when(userRepository.findByEmail("admin@example.com"))
                .thenReturn(Optional.of(admin));
        Mockito.when(userRepository.findById("student123"))
                .thenReturn(Optional.of(student));
        Mockito.when(roomRepository.findById("room123"))
                .thenReturn(Optional.of(room));
        Mockito.when(roomRepository.save(any(Room.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, String> body = new HashMap<>();
        body.put("studentId", "student123");

        mockMvc.perform(put("/api/rooms/room123/assign")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.occupantIds[0]").value("student123"));
    }

    @Test
    @WithMockUser(username = "admin@example.com")
    public void testAssignStudentCapacityExceeded() throws Exception {
        Room room = new Room();
        room.setId("room123");
        room.setCapacity(1);
        room.setRoomNumber("101");
        room.setStatus("AVAILABLE");
        room.getOccupantIds().add("other123");

        User student = new User();
        student.setId("student123");
        student.setEmail("student@example.com");

        User admin = new User();
        admin.setId("admin123");
        admin.setEmail("admin@example.com");
        admin.setRole("ADMIN");

        Mockito.when(userRepository.findByEmail("admin@example.com"))
                .thenReturn(Optional.of(admin));
        Mockito.when(userRepository.findById("student123"))
                .thenReturn(Optional.of(student));
        Mockito.when(roomRepository.findById("room123"))
                .thenReturn(Optional.of(room));

        Map<String, String> body = new HashMap<>();
        body.put("studentId", "student123");

        mockMvc.perform(put("/api/rooms/room123/assign")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "admin@example.com")
    public void testUnassignStudentSuccess() throws Exception {
        Room room = new Room();
        room.setId("room123");
        room.setCapacity(2);
        room.setRoomNumber("101");
        room.getOccupantIds().add("student123");

        User admin = new User();
        admin.setId("admin123");
        admin.setEmail("admin@example.com");
        admin.setRole("ADMIN");

        Mockito.when(userRepository.findByEmail("admin@example.com"))
                .thenReturn(Optional.of(admin));
        Mockito.when(roomRepository.findById("room123"))
                .thenReturn(Optional.of(room));
        Mockito.when(roomRepository.save(any(Room.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, String> body = new HashMap<>();
        body.put("studentId", "student123");

        mockMvc.perform(put("/api/rooms/room123/unassign")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.occupantIds").isEmpty());
    }
}
