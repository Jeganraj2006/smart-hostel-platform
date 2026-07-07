package com.hostel.hostel_backend.repositories;

import com.hostel.hostel_backend.models.AuditLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface AuditLogRepository extends MongoRepository<AuditLog, String> {
    List<AuditLog> findFirst100ByOrderByTimestampDesc();
}
