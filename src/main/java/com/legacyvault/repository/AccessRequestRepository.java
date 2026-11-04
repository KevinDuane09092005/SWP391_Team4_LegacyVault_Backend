package com.legacyvault.repository;

import com.legacyvault.entity.AccessRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccessRequestRepository extends JpaRepository<AccessRequest, String> {

    List<AccessRequest> findByStatus(String status);

    List<AccessRequest> findByStatusOrderByCreatedAtDesc(String status);

    long countByStatus(String status);
}
