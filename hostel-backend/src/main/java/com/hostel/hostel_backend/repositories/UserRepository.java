package com.hostel.hostel_backend.repositories;

import com.hostel.hostel_backend.models.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findByAccountStatus(String accountStatus);
    List<User> findByRole(String role);
}