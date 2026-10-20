package com.legacyvault.controller;

import com.legacyvault.service.BillingException;
import com.legacyvault.service.BillingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class BillingControllerTest {

    @Mock private BillingService billingService;

    @Test
    void nonOwnerRoleCannotUseBillingEndpoints() {
        BillingController controller = new BillingController(billingService);
        var authentication = new UsernamePasswordAuthenticationToken("executor-1", null,
                List.of(new SimpleGrantedAuthority("ROLE_EXECUTOR")));

        assertThrows(BillingException.class, () -> controller.listPaymentMethods(authentication));
        verifyNoInteractions(billingService);
    }
}
