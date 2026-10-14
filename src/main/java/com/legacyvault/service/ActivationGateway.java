package com.legacyvault.service;

public interface ActivationGateway {
    void requireExecutorAssignment(String activationId, String executorUserId);
}
