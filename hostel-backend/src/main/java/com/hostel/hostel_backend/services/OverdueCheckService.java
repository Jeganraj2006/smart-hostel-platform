package com.hostel.hostel_backend.services;

import com.hostel.hostel_backend.models.Attendance;
import com.hostel.hostel_backend.models.Room;
import com.hostel.hostel_backend.models.User;
import com.hostel.hostel_backend.repositories.AttendanceRepository;
import com.hostel.hostel_backend.repositories.RoomRepository;
import com.hostel.hostel_backend.repositories.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class OverdueCheckService {

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private AuditService auditService;

    @Autowired
    private NotificationService notificationService;

    /**
     * Checks for overdue student out-passes every 15 minutes.
     * Marks them OVERDUE, saves audits, and alerts wardens of that student's block.
     */
    @Scheduled(cron = "0 */15 * * * *")
    public void checkOverdueGatepasses() {
        log.info("Starting scheduled background check for overdue gatepasses...");

        LocalDateTime now = LocalDateTime.now();
        List<Attendance> overdueScans = attendanceRepository.findByStatusAndExpectedReturnAtBefore("OUT", now);

        if (overdueScans.isEmpty()) {
            log.info("No overdue gatepasses found.");
            return;
        }

        log.info("Found {} overdue gatepass(es) to process.", overdueScans.size());

        for (Attendance attendance : overdueScans) {
            attendance.setStatus("OVERDUE");
            attendanceRepository.save(attendance);

            // Fetch Student Details
            Optional<User> studentOpt = userRepository.findById(attendance.getStudentId());
            String studentName = studentOpt.map(User::getName).orElse("Unknown Student");
            String studentEmail = studentOpt.map(User::getEmail).orElse("Unknown Email");

            // Fetch Room Details
            Optional<Room> roomOpt = roomRepository.findByOccupantIdsContaining(attendance.getStudentId());
            String blockName = roomOpt.map(Room::getBlockName).orElse("Unknown Block");
            String roomNo = roomOpt.map(Room::getRoomNumber).orElse("N/A");

            // Audit Trail Log
            Map<String, String> metadata = new HashMap<>();
            metadata.put("studentId", attendance.getStudentId());
            metadata.put("studentName", studentName);
            metadata.put("expectedReturnAt", String.valueOf(attendance.getExpectedReturnAt()));
            metadata.put("blockName", blockName);
            metadata.put("roomNo", roomNo);

            auditService.log(
                "SYSTEM",
                "SYSTEM",
                "MARK_OVERDUE",
                "ATTENDANCE",
                attendance.getId(),
                metadata
            );

            // Alert Block Wardens
            List<User> wardens = userRepository.findByRole("WARDEN");
            String alertMessage = String.format(
                "⚠️ ATTENTION: Student %s (%s) from Room %s (%s Block) has exceeded their expected return time (%s) and is currently OVERDUE.",
                studentName,
                studentEmail,
                roomNo,
                blockName,
                attendance.getExpectedReturnAt()
            );

            for (User warden : wardens) {
                notificationService.sendAlert(warden.getEmail(), alertMessage);
            }
        }

        log.info("Completed scheduled background check for overdue gatepasses.");
    }
}
