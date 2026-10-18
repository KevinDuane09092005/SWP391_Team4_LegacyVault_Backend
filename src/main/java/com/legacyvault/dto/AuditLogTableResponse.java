package com.legacyvault.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class AuditLogTableResponse {

    @JsonProperty("logs")
    private List<AuditLogRow> logs;

    @JsonProperty("pagination")
    private PaginationInfo pagination;

    public AuditLogTableResponse() {}

    public AuditLogTableResponse(List<AuditLogRow> logs, PaginationInfo pagination) {
        this.logs = logs;
        this.pagination = pagination;
    }

    public List<AuditLogRow> getLogs() {
        return logs;
    }

    public void setLogs(List<AuditLogRow> logs) {
        this.logs = logs;
    }

    public PaginationInfo getPagination() {
        return pagination;
    }

    public void setPagination(PaginationInfo pagination) {
        this.pagination = pagination;
    }

    public static class PaginationInfo {
        @JsonProperty("total_elements")
        private long totalElements;

        @JsonProperty("current_page")
        private int currentPage;

        @JsonProperty("total_pages")
        private int totalPages;

        @JsonProperty("display_range")
        private String displayRange;

        public PaginationInfo() {}

        public PaginationInfo(long totalElements, int currentPage, int totalPages, String displayRange) {
            this.totalElements = totalElements;
            this.currentPage = currentPage;
            this.totalPages = totalPages;
            this.displayRange = displayRange;
        }

        public long getTotalElements() { return totalElements; }
        public void setTotalElements(long totalElements) { this.totalElements = totalElements; }
        public int getCurrentPage() { return currentPage; }
        public void setCurrentPage(int currentPage) { this.currentPage = currentPage; }
        public int getTotalPages() { return totalPages; }
        public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
        public String getDisplayRange() { return displayRange; }
        public void setDisplayRange(String displayRange) { this.displayRange = displayRange; }
    }
}
