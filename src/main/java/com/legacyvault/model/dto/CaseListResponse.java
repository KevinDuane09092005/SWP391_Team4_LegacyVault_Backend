package com.legacyvault.model.dto;

import java.util.List;

public record CaseListResponse(List<CaseSummaryResponse> data, PageMeta meta) {
}
