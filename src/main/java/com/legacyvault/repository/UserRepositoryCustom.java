package com.legacyvault.repository;

import com.legacyvault.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserRepositoryCustom {
    Page<User> searchAdminUsers(String search, String role, String status, Pageable pageable);
}
