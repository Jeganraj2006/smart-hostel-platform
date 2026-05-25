package com.hostel.hostel_backend.repositories;

import com.hostel.hostel_backend.models.Leave;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface LeaveRepository extends MongoRepository<Leave, String> {
    List<Leave> findByStudentId(String studentId);
    List<Leave> findByStatus(String status);
    List<Leave> findByStudentIdOrderByAppliedAtDesc(String studentId);
}