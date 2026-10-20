package com.legacyvault.repository;

import com.legacyvault.entity.PaymentTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, String> {
    Page<PaymentTransaction> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
}
