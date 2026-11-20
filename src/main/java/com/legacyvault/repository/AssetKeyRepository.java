package com.legacyvault.repository;

import com.legacyvault.entity.AssetKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AssetKeyRepository extends JpaRepository<AssetKey, String> {
    Optional<AssetKey> findByAssetId(String assetId);
    void deleteByAssetId(String assetId);
    boolean existsByAssetId(String assetId);
}
