package com.legacyvault.repository;

import com.legacyvault.entity.InAppNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InAppNotificationRepository extends JpaRepository<InAppNotification, String> {

    Page<InAppNotification> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    Optional<InAppNotification> findByNotificationIdAndUserId(String notificationId, String userId);

    long countByUserIdAndIsReadFalse(String userId);

    List<InAppNotification> findByUserIdAndIsReadFalse(String userId);

    @Modifying
    @Query("UPDATE InAppNotification n SET n.isRead = true WHERE n.userId = :userId AND n.isRead = false")
    void markAllAsReadByUserId(@Param("userId") String userId);
}
