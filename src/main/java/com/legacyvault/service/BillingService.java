package com.legacyvault.service;

import com.legacyvault.dto.AddPaymentMethodRequest;
import com.legacyvault.dto.BillingAddressRequest;
import com.legacyvault.dto.CheckoutRequest;
import com.legacyvault.dto.CheckoutResponse;
import com.legacyvault.dto.PaymentMethodResponse;
import com.legacyvault.dto.PaymentTransactionListResponse;
import com.legacyvault.dto.PaymentTransactionResponse;
import com.legacyvault.dto.PlanResponse;
import com.legacyvault.dto.SubscriptionResponse;
import com.legacyvault.dto.UpdatePaymentMethodRequest;
import com.legacyvault.entity.BillingCycle;
import com.legacyvault.entity.PaymentMethod;
import com.legacyvault.entity.PaymentStatus;
import com.legacyvault.entity.PaymentTransaction;
import com.legacyvault.entity.SubscriptionStatus;
import com.legacyvault.entity.UserSubscription;
import com.legacyvault.repository.PaymentMethodRepository;
import com.legacyvault.repository.PaymentTransactionRepository;
import com.legacyvault.repository.UserSubscriptionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class BillingService {

    private static final int RENEWAL_GRACE_DAYS = 7;

    private final PaymentMethodRepository paymentMethodRepository;
    private final PaymentTransactionRepository transactionRepository;
    private final UserSubscriptionRepository subscriptionRepository;
    private final PlanCatalogService planCatalog;
    private final PaymentGateway paymentGateway;
    private final NotificationService notificationService;

    public BillingService(PaymentMethodRepository paymentMethodRepository,
                          PaymentTransactionRepository transactionRepository,
                          UserSubscriptionRepository subscriptionRepository,
                          PlanCatalogService planCatalog, PaymentGateway paymentGateway,
                          NotificationService notificationService) {
        this.paymentMethodRepository = paymentMethodRepository;
        this.transactionRepository = transactionRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.planCatalog = planCatalog;
        this.paymentGateway = paymentGateway;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<PaymentMethodResponse> listPaymentMethods(String userId) {
        return paymentMethodRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId)
                .stream().map(this::toPaymentMethodResponse).toList();
    }

    public PaymentMethodResponse addPaymentMethod(String userId, AddPaymentMethodRequest request) {
        validateExpiry(request.expiryMonth(), request.expiryYear());
        if (!request.providerPaymentMethodToken().startsWith("pm_mock_")) {
            throw BillingException.badRequest("INVALID_DEMO_TOKEN", "This demo accepts only mock payment-provider tokens.");
        }
        if (paymentMethodRepository.existsByProviderPaymentMethodToken(request.providerPaymentMethodToken())) {
            throw BillingException.conflict("That payment method is already saved.");
        }

        List<PaymentMethod> existingMethods = paymentMethodRepository
                .findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId);
        boolean shouldBeDefault = request.makeDefault() || existingMethods.isEmpty();
        if (shouldBeDefault) clearDefault(existingMethods);

        BillingAddressRequest address = request.billingAddress();
        PaymentMethod method = new PaymentMethod(UUID.randomUUID().toString(), userId,
                request.providerPaymentMethodToken(), request.cardholderName().trim(),
                request.brand().toUpperCase(), request.lastFour(), request.expiryMonth(),
                request.expiryYear(), address == null ? null : address.line1(),
                address == null ? null : address.city(), address == null ? null : address.postalCode(),
                address == null || address.country() == null ? null : address.country().toUpperCase(),
                shouldBeDefault);
        return toPaymentMethodResponse(paymentMethodRepository.save(method));
    }

    public PaymentMethodResponse updatePaymentMethod(String userId, String paymentMethodId,
                                                     UpdatePaymentMethodRequest request) {
        validateExpiry(request.expiryMonth(), request.expiryYear());
        PaymentMethod method = requirePaymentMethod(userId, paymentMethodId);
        BillingAddressRequest address = request.billingAddress();
        method.updateBillingDetails(request.expiryMonth(), request.expiryYear(),
                address == null ? method.getBillingAddressLine1() : address.line1(),
                address == null ? method.getBillingCity() : address.city(),
                address == null ? method.getBillingPostalCode() : address.postalCode(),
                address == null || address.country() == null
                        ? method.getBillingCountry() : address.country().toUpperCase());
        return toPaymentMethodResponse(paymentMethodRepository.save(method));
    }

    public PaymentMethodResponse setDefaultPaymentMethod(String userId, String paymentMethodId) {
        PaymentMethod selected = requirePaymentMethod(userId, paymentMethodId);
        List<PaymentMethod> methods = paymentMethodRepository
                .findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId);
        clearDefault(methods);
        selected.setDefault(true);
        paymentMethodRepository.saveAll(methods);
        return toPaymentMethodResponse(paymentMethodRepository.save(selected));
    }

    public void removePaymentMethod(String userId, String paymentMethodId) {
        PaymentMethod method = requirePaymentMethod(userId, paymentMethodId);
        List<PaymentMethod> methods = paymentMethodRepository
                .findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId);
        PaymentMethod promote = method.isDefault()
                ? methods.stream().filter(item -> !item.getPaymentMethodId().equals(paymentMethodId))
                    .findFirst().orElse(null)
                : null;
        paymentMethodRepository.delete(method);
        if (promote != null) {
            promote.setDefault(true);
            paymentMethodRepository.save(promote);
        }
    }

    public CheckoutResponse checkout(String userId, CheckoutRequest request) {
        PlanResponse plan = planCatalog.requirePlan(request.planId());
        LocalDateTime now = LocalDateTime.now();
        if ("free".equals(plan.id())) {
            UserSubscription subscription = activateSubscription(userId, plan.id(), request.billingCycle(), now,
                    null, false);
            return new CheckoutResponse("FREE_PLAN_ACTIVATED", null, toSubscriptionResponse(subscription));
        }

        if (!Boolean.TRUE.equals(request.acceptRecurringBilling())) {
            throw BillingException.badRequest("AUTO_RENEWAL_CONSENT_REQUIRED",
                    "Confirm the monthly or yearly recurring charge before starting a paid plan.");
        }

        if (request.paymentMethodId() == null || request.paymentMethodId().isBlank()) {
            throw BillingException.badRequest("PAYMENT_METHOD_REQUIRED", "Choose a saved payment method for a paid plan.");
        }
        PaymentMethod method = requirePaymentMethod(userId, request.paymentMethodId());
        validateExpiry(method.getExpiryMonth(), method.getExpiryYear());

        var amount = planCatalog.priceFor(plan, request.billingCycle());
        String transactionId = UUID.randomUUID().toString();
        PaymentGateway.GatewayChargeResult charge = paymentGateway.charge(
                method.getProviderPaymentMethodToken(), amount, plan.currency(), transactionId);
        PaymentStatus status = charge.approved() ? PaymentStatus.SUCCEEDED : PaymentStatus.FAILED;
        PaymentTransaction transaction = new PaymentTransaction(transactionId, userId, plan.id(),
                request.billingCycle(), amount, plan.currency(), status, charge.providerReference(),
                charge.failureCode(), charge.failureMessage(), method.getCardBrand(), method.getCardLastFour());
        transaction = transactionRepository.save(transaction);

        UserSubscription subscription;
        if (charge.approved()) {
            LocalDateTime periodEnd = request.billingCycle() == BillingCycle.YEARLY
                    ? now.plusYears(1) : now.plusMonths(1);
            subscription = activateSubscription(userId, plan.id(), request.billingCycle(), now, periodEnd, true);
            notificationService.createNotification(userId, "Subscription started",
                    "Your " + plan.id() + " subscription is active and set to renew "
                            + request.billingCycle().name().toLowerCase() + ".",
                    "PAYMENT_SUCCEEDED", "Success", "/pricing");
        } else {
            subscription = currentSubscription(userId);
            notificationService.createNotification(userId, "Payment failed",
                    "Your " + plan.id() + " plan payment was declined. No plan change was made.",
                    "PAYMENT_FAILED", "Action required", "/pricing");
        }
        return new CheckoutResponse(charge.approved() ? "SUCCEEDED" : "FAILED",
                toTransactionResponse(transaction), toSubscriptionResponse(subscription));
    }

    public SubscriptionResponse getSubscription(String userId) {
        return toSubscriptionResponse(currentSubscription(userId));
    }

    public SubscriptionResponse cancelSubscription(String userId) {
        UserSubscription subscription = currentSubscription(userId);
        if (subscription.getSubscriptionId().isEmpty()) {
            throw BillingException.badRequest("NO_PAID_SUBSCRIPTION", "There is no paid subscription to cancel.");
        }
        if (subscription.getStatus() == SubscriptionStatus.ACTIVE
                || subscription.getStatus() == SubscriptionStatus.PAST_DUE) {
            subscription.cancelAutoRenewal();
            subscriptionRepository.save(subscription);
        }
        return toSubscriptionResponse(subscription);
    }

    public SubscriptionResponse updateAutoRenewal(String userId, boolean autoRenew) {
        UserSubscription subscription = currentSubscription(userId);
        if (subscription.getSubscriptionId().isEmpty() || "free".equalsIgnoreCase(subscription.getPlanId())) {
            throw BillingException.badRequest("NO_PAID_SUBSCRIPTION", "Auto-renew is only available for a paid subscription.");
        }
        if (autoRenew == subscription.isAutoRenew()) return toSubscriptionResponse(subscription);

        if (autoRenew) {
            if (subscription.getStatus() != SubscriptionStatus.ACTIVE) {
                throw BillingException.conflict("Only an active subscription can enable auto-renew.");
            }
            if (paymentMethodRepository.findByUserIdAndIsDefaultTrue(userId).isEmpty()) {
                throw BillingException.badRequest("DEFAULT_PAYMENT_METHOD_REQUIRED",
                        "Set a default payment method before enabling auto-renew.");
            }
            subscription.enableAutoRenewal();
        } else {
            if (subscription.getStatus() != SubscriptionStatus.ACTIVE
                    && subscription.getStatus() != SubscriptionStatus.PAST_DUE) {
                throw BillingException.conflict("This subscription cannot change auto-renew.");
            }
            subscription.cancelAutoRenewal();
        }

        return toSubscriptionResponse(subscriptionRepository.save(subscription));
    }

    public CheckoutResponse retrySubscriptionPayment(String userId) {
        UserSubscription subscription = subscriptionRepository.findByUserId(userId).orElse(null);
        if (subscription == null) {
            throw BillingException.conflict("There is no overdue subscription payment to retry.");
        }
        LocalDateTime now = LocalDateTime.now();
        if (subscription.getStatus() == SubscriptionStatus.PAST_DUE
                && (subscription.getGracePeriodEndsAt() == null
                || !subscription.getGracePeriodEndsAt().isAfter(now))) {
            subscription.expire();
            subscriptionRepository.save(subscription);
            return new CheckoutResponse("GRACE_PERIOD_EXPIRED", null, toSubscriptionResponse(subscription));
        }
        if (subscription.getStatus() != SubscriptionStatus.PAST_DUE) {
            throw BillingException.conflict("There is no overdue subscription payment to retry.");
        }

        PaymentTransaction transaction = chargeRenewal(subscription, now);
        UserSubscription updated = subscriptionRepository.save(subscription);
        return new CheckoutResponse(transaction.getStatus() == PaymentStatus.SUCCEEDED
                ? "RENEWED" : "RETRY_FAILED", toTransactionResponse(transaction),
                toSubscriptionResponse(updated));
    }

    public void processDueRenewals() {
        LocalDateTime now = LocalDateTime.now();
        List<UserSubscription> dueSubscriptions = subscriptionRepository
                .findByStatusAndCurrentPeriodEndsAtLessThanEqual(SubscriptionStatus.ACTIVE, now);
        for (UserSubscription subscription : dueSubscriptions) {
            if (subscription.isAutoRenew()) {
                chargeRenewal(subscription, now);
            } else {
                subscription.cancelAtPeriodEnd();
            }
            subscriptionRepository.save(subscription);
        }

        List<UserSubscription> pastGraceSubscriptions = subscriptionRepository
                .findByStatusAndGracePeriodEndsAtLessThanEqual(SubscriptionStatus.PAST_DUE, now);
        for (UserSubscription subscription : pastGraceSubscriptions) {
            subscription.expire();
            subscriptionRepository.save(subscription);
        }
    }

    @Transactional(readOnly = true)
    public PaymentTransactionListResponse listTransactions(String userId, int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw BillingException.badRequest("INVALID_PAGE", "Page must be non-negative and size must be between 1 and 100.");
        }
        PageRequest request = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<PaymentTransaction> result = transactionRepository.findByUserIdOrderByCreatedAtDesc(userId, request);
        return new PaymentTransactionListResponse(result.getContent().stream()
                .map(this::toTransactionResponse).toList(), page, size, result.getTotalElements());
    }

    private PaymentMethod requirePaymentMethod(String userId, String paymentMethodId) {
        return paymentMethodRepository.findByPaymentMethodIdAndUserId(paymentMethodId, userId)
                .orElseThrow(BillingException::notFound);
    }

    private void validateExpiry(int month, int year) {
        try {
            if (YearMonth.of(year, month).isBefore(YearMonth.now())) {
                throw BillingException.badRequest("CARD_EXPIRED", "The saved payment method has expired.");
            }
        } catch (java.time.DateTimeException exception) {
            throw BillingException.badRequest("INVALID_EXPIRY", "Enter a valid card expiry month and year.");
        }
    }

    private void clearDefault(List<PaymentMethod> methods) {
        methods.stream().filter(PaymentMethod::isDefault).forEach(method -> method.setDefault(false));
        paymentMethodRepository.saveAll(methods);
    }

    private UserSubscription currentSubscription(String userId) {
        UserSubscription subscription = subscriptionRepository.findByUserId(userId).orElse(null);
        if (subscription == null) {
            return new UserSubscription("", userId, "free", BillingCycle.MONTHLY,
                    SubscriptionStatus.ACTIVE, null, null, false);
        }
        LocalDateTime now = LocalDateTime.now();
        if (subscription.getStatus() == SubscriptionStatus.ACTIVE
                && subscription.getCurrentPeriodEndsAt() != null
                && !subscription.getCurrentPeriodEndsAt().isAfter(now)) {
            if (!subscription.isAutoRenew()) {
                subscription.cancelAtPeriodEnd();
                return subscriptionRepository.save(subscription);
            }
        }
        if (subscription.getStatus() == SubscriptionStatus.PAST_DUE
                && subscription.getGracePeriodEndsAt() != null
                && !subscription.getGracePeriodEndsAt().isAfter(now)) {
            subscription.expire();
            return subscriptionRepository.save(subscription);
        }
        return subscription;
    }

    private UserSubscription activateSubscription(String userId, String planId, BillingCycle cycle,
                                                  LocalDateTime startedAt, LocalDateTime periodEnd,
                                                  boolean autoRenew) {
        UserSubscription subscription = subscriptionRepository.findByUserId(userId)
                .orElseGet(() -> new UserSubscription(UUID.randomUUID().toString(), userId, planId,
                        cycle, SubscriptionStatus.ACTIVE, startedAt, periodEnd, autoRenew));
        subscription.activate(planId, cycle, startedAt, periodEnd, autoRenew);
        return subscriptionRepository.save(subscription);
    }

    private PaymentTransaction chargeRenewal(UserSubscription subscription, LocalDateTime now) {
        PlanResponse plan = planCatalog.requirePlan(subscription.getPlanId());
        var amount = planCatalog.priceFor(plan, subscription.getBillingCycle());
        String transactionId = UUID.randomUUID().toString();
        PaymentMethod method = paymentMethodRepository.findByUserIdAndIsDefaultTrue(subscription.getUserId())
                .orElse(null);

        PaymentGateway.GatewayChargeResult charge;
        if (method == null) {
            charge = new PaymentGateway.GatewayChargeResult(false, null,
                    "DEFAULT_PAYMENT_METHOD_MISSING", "Add a default payment method to renew your subscription.");
        } else {
            try {
                validateExpiry(method.getExpiryMonth(), method.getExpiryYear());
                charge = paymentGateway.charge(method.getProviderPaymentMethodToken(), amount,
                        plan.currency(), transactionId);
            } catch (BillingException exception) {
                charge = new PaymentGateway.GatewayChargeResult(false, null,
                        "PAYMENT_METHOD_EXPIRED", "Update your expired payment method to renew.");
            }
        }

        PaymentStatus status = charge.approved() ? PaymentStatus.SUCCEEDED : PaymentStatus.FAILED;
        PaymentTransaction transaction = transactionRepository.save(new PaymentTransaction(transactionId,
                subscription.getUserId(), plan.id(), subscription.getBillingCycle(), amount, plan.currency(),
                status, charge.providerReference(), charge.failureCode(), charge.failureMessage(),
                method == null ? null : method.getCardBrand(), method == null ? null : method.getCardLastFour()));

        if (charge.approved()) {
            LocalDateTime periodStart = subscription.getCurrentPeriodEndsAt() != null
                    && subscription.getCurrentPeriodEndsAt().isAfter(now)
                    ? subscription.getCurrentPeriodEndsAt() : now;
            LocalDateTime nextPeriodEnd = subscription.getBillingCycle() == BillingCycle.YEARLY
                    ? periodStart.plusYears(1) : periodStart.plusMonths(1);
            subscription.completeRenewal(nextPeriodEnd);
            notificationService.createNotification(subscription.getUserId(), "Subscription renewed",
                    "Your " + plan.id() + " subscription renewed successfully.",
                    "PAYMENT_SUCCEEDED", "Success", "/pricing");
        } else {
            if (subscription.getStatus() == SubscriptionStatus.ACTIVE) {
                subscription.markPastDue(now.plusDays(RENEWAL_GRACE_DAYS));
            }
            notificationService.createNotification(subscription.getUserId(), "Renewal payment failed",
                    "Your subscription payment failed. Update your payment method "
                            + "within 7 days to keep the subscription active.",
                    "PAYMENT_FAILED", "Action required", "/pricing");
        }
        return transaction;
    }

    private PaymentMethodResponse toPaymentMethodResponse(PaymentMethod method) {
        return new PaymentMethodResponse(method.getPaymentMethodId(), method.getCardholderName(),
                method.getCardBrand(), method.getCardLastFour(), method.getExpiryMonth(),
                method.getExpiryYear(), new BillingAddressRequest(method.getBillingAddressLine1(),
                method.getBillingCity(), method.getBillingPostalCode(), method.getBillingCountry()),
                method.isDefault(), method.getCreatedAt());
    }

    private SubscriptionResponse toSubscriptionResponse(UserSubscription subscription) {
        return new SubscriptionResponse(subscription.getPlanId(), subscription.getBillingCycle(),
                subscription.getStatus(), subscription.getStartedAt(),
                subscription.getCurrentPeriodEndsAt(), subscription.getGracePeriodEndsAt(),
                subscription.isAutoRenew());
    }

    private PaymentTransactionResponse toTransactionResponse(PaymentTransaction transaction) {
        return new PaymentTransactionResponse(transaction.getTransactionId(), transaction.getPlanId(),
                transaction.getBillingCycle(), transaction.getAmount(), transaction.getCurrency(),
                transaction.getStatus(), transaction.getProviderTransactionReference(),
                transaction.getFailureCode(), transaction.getFailureMessage(),
                transaction.getPaymentMethodBrand(), transaction.getPaymentMethodLastFour(),
                transaction.getCreatedAt());
    }
}
