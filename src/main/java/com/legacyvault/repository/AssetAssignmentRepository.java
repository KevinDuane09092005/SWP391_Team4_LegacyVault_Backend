package com.legacyvault.repository;

import com.legacyvault.entity.AssetAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AssetAssignmentRepository extends JpaRepository<AssetAssignment, String> {

    Optional<AssetAssignment> findByAssetId(String assetId);

    boolean existsByAssetId(String assetId);
}
