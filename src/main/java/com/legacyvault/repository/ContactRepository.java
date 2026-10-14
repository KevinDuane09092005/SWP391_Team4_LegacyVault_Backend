package com.legacyvault.repository;

import com.legacyvault.entity.Contact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContactRepository extends JpaRepository<Contact, String> {

    List<Contact> findByOwnerId(String ownerId);

    List<Contact> findByOwnerIdAndIsExecutorTrue(String ownerId);

    Optional<Contact> findByContactIdAndOwnerId(String contactId, String ownerId);

    boolean existsByContactIdAndOwnerId(String contactId, String ownerId);

    @Query("SELECT c FROM Contact c WHERE c.ownerId = :ownerId AND " +
           "(:keyword IS NULL OR :keyword = '' OR " +
           "LOWER(c.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.email) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Contact> searchContacts(@Param("ownerId") String ownerId, @Param("keyword") String keyword);
}
