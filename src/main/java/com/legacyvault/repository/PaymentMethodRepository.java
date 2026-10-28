package com.legacyvault.repository;

import com.legacyvault.entity.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, String> {
    List<PaymentMethod> findByUserIdOrderByIsDefaultDescCreatedAtDesc(String userId);
    Optional<PaymentMethod> findByPaymentMethodIdAndUserId(String paymentMethodId, String userId);
    Optional<PaymentMethod> findByUserIdAndIsDefaultTrue(String userId);
    boolean existsByProviderPaymentMethodToken(String providerPaymentMethodToken);
}
