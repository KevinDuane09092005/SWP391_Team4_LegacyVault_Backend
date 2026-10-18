package com.legacyvault.dto;

import java.util.List;

public record CaseListResponse(List<CaseSummaryResponse> data, PageMeta meta) {
}
