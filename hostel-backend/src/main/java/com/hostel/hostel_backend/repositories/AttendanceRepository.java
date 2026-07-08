package com.hostel.hostel_backend.repositories;

import com.hostel.hostel_backend.models.Attendance;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends MongoRepository<Attendance, String> {
    Optional<Attendance> findByLeaveId(String leaveId);
    List<Attendance> findByStatusAndExpectedReturnAtBefore(String status, LocalDateTime expectedReturnAt);
    List<Attendance> findByExitScannedAtAfterAndStatusIn(LocalDateTime startOfDay, List<String> statuses);
    List<Attendance> findByStudentId(String studentId);
}
