package com.hostel.hostel_backend.repositories;

import com.hostel.hostel_backend.models.Fee;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface FeeRepository extends MongoRepository<Fee, String> {
    List<Fee> findByStudentId(String studentId);
}
