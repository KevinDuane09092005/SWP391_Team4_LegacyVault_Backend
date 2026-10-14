package com.legacyvault.service;

import org.springframework.stereotype.Component;

@Component
public class UnavailableActivationGateway implements ActivationGateway {
    @Override
    public void requireExecutorAssignment(String activationId, String executorUserId) {
        throw Flow5Exception.unavailable("Flow 4 activation lookup has not been connected yet.");
    }
}
