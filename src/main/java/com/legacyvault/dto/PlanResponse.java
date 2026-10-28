package com.legacyvault.dto;

import java.math.BigDecimal;

public record PlanResponse(
        String id,
        boolean recommended,
        String currency,
        Price price,
        Limits limits
) {
    public record Price(BigDecimal monthly, BigDecimal yearly) { }
    public record Limits(int collections, int records, int trustedContacts, int attachmentsPerRecord) { }
}
