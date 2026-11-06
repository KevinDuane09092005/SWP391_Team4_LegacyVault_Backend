package com.legacyvault.repository;

import com.legacyvault.entity.ThirdPartyIntegration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ThirdPartyIntegrationRepository extends JpaRepository<ThirdPartyIntegration, String> {

    Optional<ThirdPartyIntegration> findByServiceName(String serviceName);

    boolean existsByServiceName(String serviceName);
}

