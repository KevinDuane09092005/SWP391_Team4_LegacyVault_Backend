package com.legacyvault.repository;

import com.legacyvault.entity.Contact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContactRepository extends JpaRepository<Contact, String>, ContactRepositoryCustom {

    List<Contact> findByOwnerId(String ownerId);

    List<Contact> findByOwnerIdAndIsExecutorTrue(String ownerId);

    Optional<Contact> findByContactIdAndOwnerId(String contactId, String ownerId);

    boolean existsByContactIdAndOwnerId(String contactId, String ownerId);

    boolean existsByOwnerIdAndEmailIgnoreCase(String ownerId, String email);

    List<Contact> findByEmail(String email);

    List<Contact> findByEmailIgnoreCaseAndStatus(String email, String status);

    Optional<Contact> findFirstByEmail(String email);
}
