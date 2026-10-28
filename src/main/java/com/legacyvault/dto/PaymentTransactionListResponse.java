package com.legacyvault.dto;

import java.util.List;

public record PaymentTransactionListResponse(
        List<PaymentTransactionResponse> transactions,
        int page,
        int size,
        long totalElements
) { }
