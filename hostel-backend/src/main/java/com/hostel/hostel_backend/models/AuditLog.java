package com.hostel.hostel_backend.models;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Document(collection = "audit_logs")
public class AuditLog {

    @Id
    private String id;

    private String actorId;
    private String actorRole;
    private String action;
    private String targetType;
    private String targetId;
    private LocalDateTime timestamp = LocalDateTime.now();
    private Map<String, String> metadata;
}
