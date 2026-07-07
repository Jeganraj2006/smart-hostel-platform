package com.hostel.hostel_backend.repositories;

import com.hostel.hostel_backend.models.Visitor;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface VisitorRepository extends MongoRepository<Visitor, String> {

    // Currently checked-in = checkOutAt is null
    List<Visitor> findByCheckOutAtIsNull();

    // All visits for a given host student
    List<Visitor> findByHostStudentId(String hostStudentId);
}
