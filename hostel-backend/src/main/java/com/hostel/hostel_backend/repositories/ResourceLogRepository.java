package com.hostel.hostel_backend.repositories;

import com.hostel.hostel_backend.models.ResourceLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResourceLogRepository extends MongoRepository<ResourceLog, String> {
    List<ResourceLog> findByBlockName(String blockName);
    List<ResourceLog> findByResourceType(String resourceType);
}
