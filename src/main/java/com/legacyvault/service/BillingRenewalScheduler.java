package com.legacyvault.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class BillingRenewalScheduler {

    private static final Logger logger = LoggerFactory.getLogger(BillingRenewalScheduler.class);
    private final BillingService billingService;

    public BillingRenewalScheduler(BillingService billingService) {
        this.billingService = billingService;
    }

    @Scheduled(fixedDelay = 60000, initialDelay = 15000)
    public void processDueRenewals() {
        try {
            billingService.processDueRenewals();
        } catch (RuntimeException exception) {
            logger.error("Billing renewal scan failed; it will retry on the next scheduled run.", exception);
        }
    }
}
