package com.legacyvault.service;

public interface ActivationGateway {
    VerificationTarget requireExecutorAssignment(String handoverCaseId, String assignmentId,
                                                String beneficiaryContactId, String executorUserId);
}
