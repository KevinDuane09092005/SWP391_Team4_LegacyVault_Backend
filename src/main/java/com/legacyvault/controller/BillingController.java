package com.legacyvault.controller;

import com.legacyvault.dto.AddPaymentMethodRequest;
import com.legacyvault.dto.CheckoutRequest;
import com.legacyvault.dto.CheckoutResponse;
import com.legacyvault.dto.PaymentMethodResponse;
import com.legacyvault.dto.PaymentTransactionListResponse;
import com.legacyvault.dto.SubscriptionResponse;
import com.legacyvault.dto.UpdateAutoRenewRequest;
import com.legacyvault.dto.UpdatePaymentMethodRequest;
import com.legacyvault.service.BillingException;
import com.legacyvault.service.BillingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/billing")
public class BillingController {

    private final BillingService billingService;

    public BillingController(BillingService billingService) {
        this.billingService = billingService;
    }

    @GetMapping("/subscription")
    public SubscriptionResponse getSubscription(Authentication authentication) {
        return billingService.getSubscription(ownerId(authentication));
    }

    @GetMapping("/payment-methods")
    public List<PaymentMethodResponse> listPaymentMethods(Authentication authentication) {
        return billingService.listPaymentMethods(ownerId(authentication));
    }

    @PostMapping("/payment-methods")
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentMethodResponse addPaymentMethod(Authentication authentication,
                                                  @Valid @RequestBody AddPaymentMethodRequest request) {
        return billingService.addPaymentMethod(ownerId(authentication), request);
    }

    @PatchMapping("/payment-methods/{paymentMethodId}")
    public PaymentMethodResponse updatePaymentMethod(Authentication authentication,
                                                     @PathVariable String paymentMethodId,
                                                     @Valid @RequestBody UpdatePaymentMethodRequest request) {
        return billingService.updatePaymentMethod(ownerId(authentication), paymentMethodId, request);
    }

    @PutMapping("/payment-methods/{paymentMethodId}/default")
    public PaymentMethodResponse setDefaultPaymentMethod(Authentication authentication,
                                                          @PathVariable String paymentMethodId) {
        return billingService.setDefaultPaymentMethod(ownerId(authentication), paymentMethodId);
    }

    @DeleteMapping("/payment-methods/{paymentMethodId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removePaymentMethod(Authentication authentication, @PathVariable String paymentMethodId) {
        billingService.removePaymentMethod(ownerId(authentication), paymentMethodId);
    }

    @PostMapping("/checkout")
    public CheckoutResponse checkout(Authentication authentication, @Valid @RequestBody CheckoutRequest request) {
        return billingService.checkout(ownerId(authentication), request);
    }

    @PostMapping("/subscription/cancel")
    public SubscriptionResponse cancelSubscription(Authentication authentication) {
        return billingService.cancelSubscription(ownerId(authentication));
    }

    @PutMapping("/subscription/auto-renew")
    public SubscriptionResponse updateAutoRenewal(Authentication authentication,
                                                  @Valid @RequestBody UpdateAutoRenewRequest request) {
        return billingService.updateAutoRenewal(ownerId(authentication), request.autoRenew());
    }

    @PostMapping("/subscription/retry-payment")
    public CheckoutResponse retrySubscriptionPayment(Authentication authentication) {
        return billingService.retrySubscriptionPayment(ownerId(authentication));
    }

    @GetMapping("/transactions")
    public PaymentTransactionListResponse listTransactions(Authentication authentication,
                                                            @RequestParam(defaultValue = "0") int page,
                                                            @RequestParam(defaultValue = "20") int size) {
        return billingService.listTransactions(ownerId(authentication), page, size);
    }

    private String ownerId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication.getName() == null) {
            throw BillingException.unauthorized();
        }
        boolean isOwner = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> "ROLE_VAULT_OWNER".equalsIgnoreCase(authority));
        if (!isOwner) throw BillingException.forbidden();
        return authentication.getName();
    }
}
