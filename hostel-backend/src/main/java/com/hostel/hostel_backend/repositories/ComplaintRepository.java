package com.hostel.hostel_backend.repositories;

import com.hostel.hostel_backend.models.Complaint;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

import java.time.LocalDateTime;

public interface ComplaintRepository extends MongoRepository<Complaint, String> {
    List<Complaint> findByStudentId(String studentId);
    List<Complaint> findByStatus(String status);
    List<Complaint> findByRaisedAtAfter(LocalDateTime raisedAtAfter);
    long countByAssetIdAndCategoryAndRaisedAtAfter(String assetId, String category, LocalDateTime raisedAtAfter);
}