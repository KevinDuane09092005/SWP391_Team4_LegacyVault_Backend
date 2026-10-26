package com.legacyvault.controller;

import com.legacyvault.dto.AdminUserListResponse;
import com.legacyvault.dto.AdminUserResponse;
import com.legacyvault.dto.UpdateAdminUserRoleRequest;
import com.legacyvault.dto.UpdateAdminUserStatusRequest;
import com.legacyvault.service.AdminUserManagementService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUserManagementController {

    private final AdminUserManagementService service;

    public AdminUserManagementController(AdminUserManagementService service) {
        this.service = service;
    }

    @GetMapping
    public AdminUserListResponse listUsers(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "ALL") String role,
            @RequestParam(defaultValue = "ALL") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.listUsers(search, role, status, page, size);
    }

    @PatchMapping("/{userId}/role")
    public AdminUserResponse updateRole(
            @PathVariable String userId,
            @Valid @RequestBody UpdateAdminUserRoleRequest request,
            Authentication authentication) {
        return service.updateRole(authentication.getName(), userId, request.role());
    }

    @PatchMapping("/{userId}/status")
    public AdminUserResponse updateStatus(
            @PathVariable String userId,
            @Valid @RequestBody UpdateAdminUserStatusRequest request,
            Authentication authentication) {
        return service.updateStatus(authentication.getName(), userId, request.status());
    }
}
