package com.legacyvault.dto;

import java.util.List;

public record AdminUserPageResponse(List<AdminUserResponse> users, Pagination pagination) {
    public record Pagination(int page, int size, long totalElements, int totalPages, int currentPage) {
        public Pagination(int page, int size, long totalElements, int totalPages) {
            this(page, size, totalElements, totalPages, page);
        }
    }
}

