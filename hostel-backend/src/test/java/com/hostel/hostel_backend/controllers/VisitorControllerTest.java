package com.hostel.hostel_backend.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hostel.hostel_backend.models.User;
import com.hostel.hostel_backend.models.Visitor;
import com.hostel.hostel_backend.repositories.UserRepository;
import com.hostel.hostel_backend.repositories.VisitorRepository;
import com.hostel.hostel_backend.services.AuditService;
import com.hostel.hostel_backend.services.CloudinaryService;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VisitorController.class)
@AutoConfigureMockMvc(addFilters = false)
public class VisitorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private VisitorRepository visitorRepository;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private AuditService auditService;

    @MockBean
    private CloudinaryService cloudinaryService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @MockBean
    private RateLimitFilter rateLimitFilter;

    private User guardUser;
    private Visitor activeVisitor;

    @BeforeEach
    public void setUp() {
        guardUser = new User();
        guardUser.setId("guard1");
        guardUser.setEmail("guard@hostel.com");
        guardUser.setRole("SECURITY_GUARD");
        guardUser.setAccountStatus("ACTIVE");
        guardUser.setActive(true);

        activeVisitor = new Visitor();
        activeVisitor.setId("visitor1");
        activeVisitor.setVisitorName("John Doe");
        activeVisitor.setVisitorPhone("1234567890");
        activeVisitor.setPurpose("Delivery");
        activeVisitor.setHostStudentId("student123");
        activeVisitor.setCheckInAt(LocalDateTime.now());

        // Mock Security Context
        Authentication authentication = Mockito.mock(Authentication.class);
        when(authentication.getName()).thenReturn("guard@hostel.com");
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findByEmail("guard@hostel.com")).thenReturn(Optional.of(guardUser));
    }

    @Test
    public void testCheckInSuccess() throws Exception {
        VisitorController.CheckInRequest req = new VisitorController.CheckInRequest();
        req.setVisitorName("John Doe");
        req.setVisitorPhone("1234567890");
        req.setPurpose("Delivery");
        req.setHostStudentId("student123");

        when(visitorRepository.save(any(Visitor.class))).thenAnswer(invocation -> {
            Visitor v = invocation.getArgument(0);
            v.setId("visitor_new");
            return v;
        });

        mockMvc.perform(post("/api/visitors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("visitor_new"))
                .andExpect(jsonPath("$.visitorName").value("John Doe"))
                .andExpect(jsonPath("$.approvedBy").value("guard1"));

        verify(visitorRepository, times(1)).save(any(Visitor.class));
        verify(auditService, times(1)).log(
                eq("guard1"),
                eq("SECURITY_GUARD"),
                eq("VISITOR_CHECK_IN"),
                eq("VISITOR"),
                eq("visitor_new"),
                any()
        );
    }

    @Test
    public void testCheckOutSuccess() throws Exception {
        when(visitorRepository.findById("visitor1")).thenReturn(Optional.of(activeVisitor));
        when(visitorRepository.save(any(Visitor.class))).thenAnswer(i -> i.getArgument(0));

        mockMvc.perform(put("/api/visitors/visitor1/checkout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checkOutAt").isNotEmpty());

        verify(visitorRepository, times(1)).save(activeVisitor);
        verify(auditService, times(1)).log(
                eq("guard1"),
                eq("SECURITY_GUARD"),
                eq("VISITOR_CHECK_OUT"),
                eq("VISITOR"),
                eq("visitor1"),
                any()
        );
    }

    @Test
    public void testCheckOutAlreadyCheckedOutConflict() throws Exception {
        activeVisitor.setCheckOutAt(LocalDateTime.now().minusHours(1));
        when(visitorRepository.findById("visitor1")).thenReturn(Optional.of(activeVisitor));

        mockMvc.perform(put("/api/visitors/visitor1/checkout"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Visitor has already checked out."));

        verify(visitorRepository, never()).save(any());
    }

    @Test
    public void testGetActiveVisitors() throws Exception {
        when(visitorRepository.findByCheckOutAtIsNull()).thenReturn(Collections.singletonList(activeVisitor));

        mockMvc.perform(get("/api/visitors/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].visitorName").value("John Doe"));
    }

    @Test
    public void testUploadPhotoSuccess() throws Exception {
        when(visitorRepository.findById("visitor1")).thenReturn(Optional.of(activeVisitor));
        when(cloudinaryService.uploadImage(any(), anyString())).thenReturn("https://cloudinary.com/visitor1.jpg");
        when(visitorRepository.save(any(Visitor.class))).thenAnswer(i -> i.getArgument(0));

        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", MediaType.IMAGE_JPEG_VALUE, "dummy image content".getBytes()
        );

        mockMvc.perform(multipart("/api/visitors/visitor1/photo").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.photoUrl").value("https://cloudinary.com/visitor1.jpg"));

        verify(visitorRepository, times(1)).save(activeVisitor);
        verify(auditService, times(1)).log(
                eq("guard1"),
                eq("SECURITY_GUARD"),
                eq("VISITOR_PHOTO_UPLOADED"),
                eq("VISITOR"),
                eq("visitor1"),
                any()
        );
    }
}
