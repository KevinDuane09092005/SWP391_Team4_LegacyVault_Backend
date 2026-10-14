package com.legacyvault.repository;

import com.legacyvault.entity.Asset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssetRepository extends JpaRepository<Asset, String> {
    
    List<Asset> findByOwnerId(String ownerId);
    
    Optional<Asset> findByAssetIdAndOwnerId(String assetId, String ownerId);
    
    boolean existsByAssetIdAndOwnerId(String assetId, String ownerId);
}
