package com.legacyvault.repository;

import com.legacyvault.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    long countByRoleIgnoreCase(String role);

    @Query("SELECT u FROM User u WHERE " +
            "(:search IS NULL OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
            "(:role IS NULL OR UPPER(u.role) = UPPER(:role) OR " +
            "(:role = 'EXECUTOR' AND UPPER(u.role) = 'DIGITAL_EXECUTOR')) AND " +
            "(:status IS NULL OR :status = 'ALL' OR " +
            "(:status = 'ACTIVE' AND (u.isActive = true OR u.isActive IS NULL)) OR " +
            "(:status = 'SUSPENDED' AND u.isActive = false))")
    Page<User> searchAdminUsers(
            @Param("search") String search,
            @Param("role") String role,
            @Param("status") String status,
            Pageable pageable);
}
