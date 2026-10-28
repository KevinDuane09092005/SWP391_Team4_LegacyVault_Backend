package com.legacyvault.controller;

import com.legacyvault.dto.AdminUserPageResponse;
import com.legacyvault.dto.AdminUserResponse;
import com.legacyvault.dto.AdminUserRoleRequest;
import com.legacyvault.dto.AdminUserStatusRequest;
import com.legacyvault.service.AdminUserService;
import jakarta.servlet.http.HttpServletRequest;
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
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public AdminUserPageResponse listUsers(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "ALL") String role,
            @RequestParam(defaultValue = "ALL") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        return adminUserService.listUsers(search, role, status, page, size);
    }

    @PatchMapping("/{userId}/status")
    public AdminUserResponse updateStatus(
            @PathVariable String userId,
            @Valid @RequestBody AdminUserStatusRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        return adminUserService.updateStatus(
                authentication.getName(), userId, request.status(), httpRequest.getRemoteAddr());
    }

    @PatchMapping("/{userId}/role")
    public AdminUserResponse updateRole(
            @PathVariable String userId,
            @Valid @RequestBody AdminUserRoleRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        return adminUserService.updateRole(
                authentication.getName(), userId, request.role(), httpRequest.getRemoteAddr());
    }
}

