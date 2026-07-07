package com.hostel.hostel_backend.services;

import com.hostel.hostel_backend.models.Attendance;
import com.hostel.hostel_backend.models.Room;
import com.hostel.hostel_backend.models.User;
import com.hostel.hostel_backend.repositories.AttendanceRepository;
import com.hostel.hostel_backend.repositories.RoomRepository;
import com.hostel.hostel_backend.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class OverdueCheckServiceTest {

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private OverdueCheckService overdueCheckService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testCheckOverdueGatepassesSuccessfully() {
        LocalDateTime expectedReturn = LocalDateTime.now().minusHours(1);
        
        Attendance attendance = new Attendance();
        attendance.setId("att-123");
        attendance.setLeaveId("leave-123");
        attendance.setStudentId("student-123");
        attendance.setStatus("OUT");
        attendance.setExpectedReturnAt(expectedReturn);

        List<Attendance> overdueList = new ArrayList<>();
        overdueList.add(attendance);

        // Stub findByStatusAndExpectedReturnAtBefore
        when(attendanceRepository.findByStatusAndExpectedReturnAtBefore(eq("OUT"), any(LocalDateTime.class)))
                .thenReturn(overdueList);

        // Stub User (student)
        User student = new User();
        student.setId("student-123");
        student.setName("Jane Doe");
        student.setEmail("jane@college.edu");
        when(userRepository.findById("student-123")).thenReturn(Optional.of(student));

        // Stub Room
        Room room = new Room();
        room.setId("room-123");
        room.setBlockName("Netaji Block");
        room.setRoomNumber("304");
        when(roomRepository.findByOccupantIdsContaining("student-123")).thenReturn(Optional.of(room));

        // Stub Warden User
        User warden = new User();
        warden.setId("warden-999");
        warden.setName("Warden John");
        warden.setEmail("warden.john@hostel.com");
        warden.setRole("WARDEN");
        List<User> wardens = new ArrayList<>();
        wardens.add(warden);
        when(userRepository.findByRole("WARDEN")).thenReturn(wardens);

        // Execute scheduled method
        overdueCheckService.checkOverdueGatepasses();

        // Verify status change and save
        assertEquals("OVERDUE", attendance.getStatus());
        verify(attendanceRepository, times(1)).save(attendance);

        // Verify Audit Log
        verify(auditService, times(1)).log(
                eq("SYSTEM"),
                eq("SYSTEM"),
                eq("MARK_OVERDUE"),
                eq("ATTENDANCE"),
                eq("att-123"),
                anyMap()
        );

        // Verify notification sent to warden
        verify(notificationService, times(1)).sendAlert(
                eq("warden.john@hostel.com"),
                contains("OVERDUE")
        );
    }
}
