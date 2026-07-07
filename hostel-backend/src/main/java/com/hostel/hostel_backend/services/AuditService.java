package com.hostel.hostel_backend.services;

import com.hostel.hostel_backend.models.AuditLog;
import com.hostel.hostel_backend.repositories.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class AuditService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    public void log(String actorId, String actorRole, String action,
                    String targetType, String targetId, Map<String, String> metadata) {
        AuditLog log = new AuditLog();
        log.setActorId(actorId);
        log.setActorRole(actorRole);
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setTimestamp(LocalDateTime.now());
        log.setMetadata(metadata);

        auditLogRepository.save(log);
    }
}
