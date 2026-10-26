package com.legacyvault.dto;

import java.util.List;

public record AdminUserListResponse(
        List<AdminUserResponse> users,
        Pagination pagination) {

    public record Pagination(int page, int size, long totalElements, int totalPages) {
    }
}
