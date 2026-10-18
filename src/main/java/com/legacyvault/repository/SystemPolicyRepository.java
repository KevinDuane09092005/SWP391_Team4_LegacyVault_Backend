package com.legacyvault.repository;

import com.legacyvault.entity.SystemPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SystemPolicyRepository extends JpaRepository<SystemPolicy, String> {
}
