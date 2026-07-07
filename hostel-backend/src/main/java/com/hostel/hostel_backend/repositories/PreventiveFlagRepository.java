package com.hostel.hostel_backend.repositories;

import com.hostel.hostel_backend.models.PreventiveFlag;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PreventiveFlagRepository extends MongoRepository<PreventiveFlag, String> {
    Optional<PreventiveFlag> findByAssetIdAndCategoryAndResolved(String assetId, String category, boolean resolved);
    List<PreventiveFlag> findByResolved(boolean resolved);
}
