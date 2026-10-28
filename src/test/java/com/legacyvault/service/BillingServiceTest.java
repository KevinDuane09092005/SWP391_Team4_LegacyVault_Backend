package com.legacyvault.service;

import com.legacyvault.dto.AddPaymentMethodRequest;
import com.legacyvault.dto.BillingAddressRequest;
import com.legacyvault.dto.CheckoutRequest;
import com.legacyvault.entity.BillingCycle;
import com.legacyvault.entity.PaymentMethod;
import com.legacyvault.entity.PaymentStatus;
import com.legacyvault.entity.PaymentTransaction;
import com.legacyvault.entity.SubscriptionStatus;
import com.legacyvault.entity.UserSubscription;
import com.legacyvault.repository.PaymentMethodRepository;
import com.legacyvault.repository.PaymentTransactionRepository;
import com.legacyvault.repository.UserSubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.YearMonth;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BillingServiceTest {

    @Mock private PaymentMethodRepository paymentMethodRepository;
    @Mock private PaymentTransactionRepository transactionRepository;
    @Mock private UserSubscriptionRepository subscriptionRepository;
    @Mock private PaymentGateway paymentGateway;
    @Mock private NotificationService notificationService;

    private BillingService billingService;
    private PaymentMethod paymentMethod;

    @BeforeEach
    void setUp() {
        billingService = new BillingService(paymentMethodRepository, transactionRepository,
                subscriptionRepository, new PlanCatalogService(), paymentGateway, notificationService);
        YearMonth expiry = YearMonth.now().plusYears(2);
        paymentMethod = new PaymentMethod("method-1", "user-1", "pm_mock_success_4242",
                "Demo Owner", "VISA", "4242", expiry.getMonthValue(), expiry.getYear(),
                null, null, null, null, true);
    }

    @Test
    void firstPaymentMethodBecomesDefaultAndResponseIsMasked() {
        when(paymentMethodRepository.existsByProviderPaymentMethodToken("pm_mock_success_4242"))
                .thenReturn(false);
        when(paymentMethodRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc("user-1"))
                .thenReturn(List.of());
        when(paymentMethodRepository.save(any(PaymentMethod.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var expiry = YearMonth.now().plusYears(2);
        var response = billingService.addPaymentMethod("user-1", new AddPaymentMethodRequest(
                "pm_mock_success_4242", "Demo Owner", "visa", "4242",
                expiry.getMonthValue(), expiry.getYear(),
                new BillingAddressRequest("1 Demo Street", "Ho Chi Minh City", "700000", "vn"), false));

        assertTrue(response.isDefault());
        assertEquals("4242", response.lastFour());
        assertEquals("VISA", response.brand());
        assertEquals("VN", response.billingAddress().country());
        verify(paymentMethodRepository).save(any(PaymentMethod.class));
    }

    @Test
    void successfulCheckoutUsesServerPriceAndActivatesPlan() {
        when(paymentMethodRepository.findByPaymentMethodIdAndUserId("method-1", "user-1"))
                .thenReturn(Optional.of(paymentMethod));
        when(paymentGateway.charge(eq("pm_mock_success_4242"), any(), eq("USD"), anyString()))
                .thenReturn(new PaymentGateway.GatewayChargeResult(true, "mock_tx_123", null, null));
        when(transactionRepository.save(any(PaymentTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(subscriptionRepository.findByUserId("user-1")).thenReturn(Optional.empty());
        when(subscriptionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = billingService.checkout("user-1",
                new CheckoutRequest("plus", BillingCycle.MONTHLY, "method-1", true));

        assertEquals("SUCCEEDED", response.result());
        assertEquals(PaymentStatus.SUCCEEDED, response.transaction().status());
        assertEquals("8.99", response.transaction().amount().toPlainString());
        assertEquals("plus", response.subscription().planId());
        assertTrue(response.subscription().autoRenew());
        verify(notificationService).createNotification(eq("user-1"), eq("Subscription started"),
                anyString(), eq("PAYMENT_SUCCEEDED"), eq("Success"), anyString());
    }

    @Test
    void declinedCheckoutRecordsFailureAndDoesNotChangePlan() {
        when(paymentMethodRepository.findByPaymentMethodIdAndUserId("method-1", "user-1"))
                .thenReturn(Optional.of(paymentMethod));
        when(paymentGateway.charge(eq("pm_mock_success_4242"), any(), eq("USD"), anyString()))
                .thenReturn(new PaymentGateway.GatewayChargeResult(false, null,
                        "DEMO_DECLINED", "The demo payment was declined."));
        when(transactionRepository.save(any(PaymentTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(subscriptionRepository.findByUserId("user-1")).thenReturn(Optional.empty());

        var response = billingService.checkout("user-1",
                new CheckoutRequest("essentials", BillingCycle.YEARLY, "method-1", true));

        assertEquals("FAILED", response.result());
        assertEquals(PaymentStatus.FAILED, response.transaction().status());
        assertEquals("49.00", response.transaction().amount().toPlainString());
        assertEquals("free", response.subscription().planId());
        verify(subscriptionRepository, never()).save(any());
        verify(notificationService).createNotification(eq("user-1"), eq("Payment failed"),
                anyString(), eq("PAYMENT_FAILED"), eq("Action required"), anyString());
    }

    @Test
    void successfulRenewalExtendsTheAccountSubscription() {
        LocalDateTime periodEnd = LocalDateTime.now().minusMinutes(1);
        UserSubscription subscription = new UserSubscription("sub-1", "user-1", "plus",
                BillingCycle.MONTHLY, SubscriptionStatus.ACTIVE, periodEnd.minusMonths(1), periodEnd, true);
        when(subscriptionRepository.findByStatusAndCurrentPeriodEndsAtLessThanEqual(
                eq(SubscriptionStatus.ACTIVE), any())).thenReturn(List.of(subscription));
        when(paymentMethodRepository.findByUserIdAndIsDefaultTrue("user-1"))
                .thenReturn(Optional.of(paymentMethod));
        when(paymentGateway.charge(eq("pm_mock_success_4242"), any(), eq("USD"), anyString()))
                .thenReturn(new PaymentGateway.GatewayChargeResult(true, "mock-renewal", null, null));
        when(transactionRepository.save(any(PaymentTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(subscriptionRepository.save(any(UserSubscription.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        billingService.processDueRenewals();

        assertEquals(SubscriptionStatus.ACTIVE, subscription.getStatus());
        assertTrue(subscription.isAutoRenew());
        assertTrue(subscription.getCurrentPeriodEndsAt().isAfter(LocalDateTime.now().plusDays(27)));
        assertNull(subscription.getGracePeriodEndsAt());
        verify(transactionRepository).save(argThat(transaction ->
                transaction.getStatus() == PaymentStatus.SUCCEEDED
                        && transaction.getAmount().toPlainString().equals("8.99")));
        verify(notificationService).createNotification(eq("user-1"), eq("Subscription renewed"),
                anyString(), eq("PAYMENT_SUCCEEDED"), eq("Success"), anyString());
    }

    @Test
    void failedRenewalStartsGraceWithoutRemovingThePlan() {
        LocalDateTime periodEnd = LocalDateTime.now().minusMinutes(1);
        UserSubscription subscription = new UserSubscription("sub-1", "user-1", "plus",
                BillingCycle.MONTHLY, SubscriptionStatus.ACTIVE, periodEnd.minusMonths(1), periodEnd, true);
        when(subscriptionRepository.findByStatusAndCurrentPeriodEndsAtLessThanEqual(
                eq(SubscriptionStatus.ACTIVE), any())).thenReturn(List.of(subscription));
        when(paymentMethodRepository.findByUserIdAndIsDefaultTrue("user-1"))
                .thenReturn(Optional.of(paymentMethod));
        when(paymentGateway.charge(eq("pm_mock_success_4242"), any(), eq("USD"), anyString()))
                .thenReturn(new PaymentGateway.GatewayChargeResult(false, null,
                        "DEMO_DECLINED", "The demo payment was declined."));
        when(transactionRepository.save(any(PaymentTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(subscriptionRepository.save(any(UserSubscription.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        billingService.processDueRenewals();

        assertEquals(SubscriptionStatus.PAST_DUE, subscription.getStatus());
        assertEquals("plus", subscription.getPlanId());
        assertTrue(subscription.getGracePeriodEndsAt().isAfter(LocalDateTime.now().plusDays(6)));
        assertTrue(subscription.isAutoRenew());
        verify(notificationService).createNotification(eq("user-1"), eq("Renewal payment failed"),
                anyString(), eq("PAYMENT_FAILED"), eq("Action required"), anyString());
    }

    @Test
    void ownerCanRetryFailedRenewalDuringGracePeriod() {
        LocalDateTime now = LocalDateTime.now();
        UserSubscription subscription = new UserSubscription("sub-1", "user-1", "essentials",
                BillingCycle.YEARLY, SubscriptionStatus.PAST_DUE, now.minusYears(1), now.minusDays(1), true);
        subscription.markPastDue(now.plusDays(6));
        when(subscriptionRepository.findByUserId("user-1")).thenReturn(Optional.of(subscription));
        when(paymentMethodRepository.findByUserIdAndIsDefaultTrue("user-1"))
                .thenReturn(Optional.of(paymentMethod));
        when(paymentGateway.charge(eq("pm_mock_success_4242"), any(), eq("USD"), anyString()))
                .thenReturn(new PaymentGateway.GatewayChargeResult(true, "mock-retry", null, null));
        when(transactionRepository.save(any(PaymentTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(subscriptionRepository.save(any(UserSubscription.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = billingService.retrySubscriptionPayment("user-1");

        assertEquals("RENEWED", response.result());
        assertEquals(SubscriptionStatus.ACTIVE, response.subscription().status());
        assertTrue(response.subscription().autoRenew());
        assertNull(response.subscription().gracePeriodEndsAt());
    }

    @Test
    void retryAfterGracePeriodExpiresSubscriptionWithoutCharging() {
        LocalDateTime now = LocalDateTime.now();
        UserSubscription subscription = new UserSubscription("sub-1", "user-1", "plus",
                BillingCycle.MONTHLY, SubscriptionStatus.PAST_DUE, now.minusMonths(2), now.minusDays(8), true);
        subscription.markPastDue(now.minusMinutes(1));
        when(subscriptionRepository.findByUserId("user-1")).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.save(any(UserSubscription.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = billingService.retrySubscriptionPayment("user-1");

        assertEquals("GRACE_PERIOD_EXPIRED", response.result());
        assertEquals(SubscriptionStatus.EXPIRED, response.subscription().status());
        verifyNoInteractions(paymentGateway);
    }

    @Test
    void cancelTurnsOffFutureRenewalButKeepsCurrentPeriodActive() {
        LocalDateTime now = LocalDateTime.now();
        UserSubscription subscription = new UserSubscription("sub-1", "user-1", "plus",
                BillingCycle.MONTHLY, SubscriptionStatus.ACTIVE, now.minusDays(1), now.plusDays(29), true);
        when(subscriptionRepository.findByUserId("user-1")).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.save(any(UserSubscription.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = billingService.cancelSubscription("user-1");

        assertEquals(SubscriptionStatus.ACTIVE, response.status());
        assertFalse(response.autoRenew());
        assertTrue(response.currentPeriodEndsAt().isAfter(now));
    }

    @Test
    void enablingAutoRenewRequiresActivePaidSubscriptionAndDefaultMethod() {
        LocalDateTime now = LocalDateTime.now();
        UserSubscription subscription = new UserSubscription("sub-1", "user-1", "plus",
                BillingCycle.MONTHLY, SubscriptionStatus.ACTIVE, now.minusDays(1), now.plusDays(29), false);
        when(subscriptionRepository.findByUserId("user-1")).thenReturn(Optional.of(subscription));
        when(paymentMethodRepository.findByUserIdAndIsDefaultTrue("user-1"))
                .thenReturn(Optional.of(paymentMethod));
        when(subscriptionRepository.save(any(UserSubscription.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = billingService.updateAutoRenewal("user-1", true);

        assertTrue(response.autoRenew());
        assertEquals(SubscriptionStatus.ACTIVE, response.status());
        verify(subscriptionRepository).save(subscription);
    }

    @Test
    void enablingAutoRenewWithoutDefaultMethodIsRejected() {
        LocalDateTime now = LocalDateTime.now();
        UserSubscription subscription = new UserSubscription("sub-1", "user-1", "plus",
                BillingCycle.MONTHLY, SubscriptionStatus.ACTIVE, now.minusDays(1), now.plusDays(29), false);
        when(subscriptionRepository.findByUserId("user-1")).thenReturn(Optional.of(subscription));
        when(paymentMethodRepository.findByUserIdAndIsDefaultTrue("user-1")).thenReturn(Optional.empty());

        BillingException exception = assertThrows(BillingException.class,
                () -> billingService.updateAutoRenewal("user-1", true));

        assertEquals("DEFAULT_PAYMENT_METHOD_REQUIRED", exception.getCode());
        assertFalse(subscription.isAutoRenew());
        verify(subscriptionRepository, never()).save(any(UserSubscription.class));
    }

    @Test
    void expiredGracePeriodExpiresSubscriptionWithoutDeletingItsAccountRecord() {
        LocalDateTime now = LocalDateTime.now();
        UserSubscription subscription = new UserSubscription("sub-1", "user-1", "plus",
                BillingCycle.MONTHLY, SubscriptionStatus.PAST_DUE, now.minusMonths(2), now.minusDays(8), true);
        subscription.markPastDue(now.minusDays(1));
        when(subscriptionRepository.findByStatusAndGracePeriodEndsAtLessThanEqual(
                eq(SubscriptionStatus.PAST_DUE), any())).thenReturn(List.of(subscription));
        when(subscriptionRepository.save(any(UserSubscription.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        billingService.processDueRenewals();

        assertEquals(SubscriptionStatus.EXPIRED, subscription.getStatus());
        assertEquals("user-1", subscription.getUserId());
        verify(subscriptionRepository).save(subscription);
        verifyNoInteractions(paymentGateway);
    }

    @Test
    void checkoutCannotUseAnotherOwnersPaymentMethod() {
        when(paymentMethodRepository.findByPaymentMethodIdAndUserId("method-1", "user-2"))
                .thenReturn(Optional.empty());

        assertThrows(BillingException.class, () -> billingService.checkout("user-2",
                new CheckoutRequest("plus", BillingCycle.MONTHLY, "method-1", true)));
        verifyNoInteractions(paymentGateway);
    }

    @Test
    void paidCheckoutRequiresExplicitRecurringBillingConsent() {
        assertThrows(BillingException.class, () -> billingService.checkout("user-1",
                new CheckoutRequest("plus", BillingCycle.MONTHLY, "method-1", false)));
        verifyNoInteractions(paymentGateway, paymentMethodRepository);
    }

    @Test
    void mockTokenCannotContainOrRepresentRawCardInput() {
        var expiry = YearMonth.now().plusYears(2);
        assertThrows(BillingException.class, () -> billingService.addPaymentMethod("user-1",
                new AddPaymentMethodRequest("4111111111111111", "Demo Owner", "visa", "1111",
                        expiry.getMonthValue(), expiry.getYear(), null, false)));
    }
}
