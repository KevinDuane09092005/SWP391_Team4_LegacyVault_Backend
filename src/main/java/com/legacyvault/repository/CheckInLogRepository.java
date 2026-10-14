package com.legacyvault.repository;

import com.legacyvault.entity.CheckInLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CheckInLogRepository extends JpaRepository<CheckInLog, String> {

    List<CheckInLog> findByOwnerIdOrderByRecordedAtDesc(String ownerId);

    List<CheckInLog> findByDmsIdOrderByRecordedAtDesc(String dmsId);
}
