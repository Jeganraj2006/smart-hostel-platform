package com.hostel.hostel_backend.models;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "fees")
public class Fee {

    @Id
    private String id;

    private String studentId;
    private String feeType; // HOSTEL, MESS, MAINTENANCE
    private Double amount;
    private String dueDate;
    private String status; // PAID, PENDING, OVERDUE
    private String paidDate;
}