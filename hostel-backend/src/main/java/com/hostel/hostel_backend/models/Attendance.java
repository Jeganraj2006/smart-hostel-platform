package com.hostel.hostel_backend.models;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "attendance")
public class Attendance {

    @Id
    private String id;

    private String leaveId;
    private String studentId;

    private LocalDateTime exitScannedAt;
    private LocalDateTime entryScannedAt;
    private LocalDateTime expectedReturnAt;

    private String status; // OUT, RETURNED, OVERDUE
}
