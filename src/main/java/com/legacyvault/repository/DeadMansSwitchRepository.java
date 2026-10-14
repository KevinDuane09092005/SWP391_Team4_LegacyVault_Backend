package com.legacyvault.repository;

import com.legacyvault.entity.DeadMansSwitch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeadMansSwitchRepository extends JpaRepository<DeadMansSwitch, String> {

    Optional<DeadMansSwitch> findByOwnerId(String ownerId);

    List<DeadMansSwitch> findByStatus(String status);

    List<DeadMansSwitch> findByStatusIn(List<String> statuses);
}
