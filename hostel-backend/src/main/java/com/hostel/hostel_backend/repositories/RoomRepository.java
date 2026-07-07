package com.hostel.hostel_backend.repositories;

import com.hostel.hostel_backend.models.Room;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

import java.util.Optional;

public interface RoomRepository extends MongoRepository<Room, String> {
    List<Room> findByBlockName(String blockName);
    List<Room> findByStatus(String status);
    Optional<Room> findByOccupantIdsContaining(String studentId);
}
