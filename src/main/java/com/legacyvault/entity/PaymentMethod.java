package com.legacyvault.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment_methods")
public class PaymentMethod {

    @Id
    @Column(name = "payment_method_id", nullable = false, updatable = false, length = 36,
            columnDefinition = "CHAR(36)")
    private String paymentMethodId;

    @Column(name = "user_id", nullable = false, length = 36, columnDefinition = "CHAR(36)")
    private String userId;

    @Column(name = "provider", nullable = false, length = 32)
    private String provider = "MOCK";

    @Column(name = "provider_payment_method_token", nullable = false, unique = true, length = 128)
    private String providerPaymentMethodToken;

    @Column(name = "cardholder_name", nullable = false, length = 100)
    private String cardholderName;

    @Column(name = "card_brand", nullable = false, length = 24)
    private String cardBrand;

    @Column(name = "card_last_four", nullable = false, length = 4)
    private String cardLastFour;

    @Column(name = "expiry_month", nullable = false)
    private int expiryMonth;

    @Column(name = "expiry_year", nullable = false)
    private int expiryYear;

    @Column(name = "billing_address_line1", length = 200)
    private String billingAddressLine1;

    @Column(name = "billing_city", length = 100)
    private String billingCity;

    @Column(name = "billing_postal_code", length = 20)
    private String billingPostalCode;

    @Column(name = "billing_country", length = 2)
    private String billingCountry;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(nullable = false)
    private int version;

    protected PaymentMethod() {
    }

    public PaymentMethod(String paymentMethodId, String userId, String providerPaymentMethodToken,
                         String cardholderName, String cardBrand, String cardLastFour,
                         int expiryMonth, int expiryYear, String billingAddressLine1,
                         String billingCity, String billingPostalCode, String billingCountry,
                         boolean isDefault) {
        this.paymentMethodId = paymentMethodId;
        this.userId = userId;
        this.providerPaymentMethodToken = providerPaymentMethodToken;
        this.cardholderName = cardholderName;
        this.cardBrand = cardBrand;
        this.cardLastFour = cardLastFour;
        this.expiryMonth = expiryMonth;
        this.expiryYear = expiryYear;
        this.billingAddressLine1 = billingAddressLine1;
        this.billingCity = billingCity;
        this.billingPostalCode = billingPostalCode;
        this.billingCountry = billingCountry;
        this.isDefault = isDefault;
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public String getPaymentMethodId() { return paymentMethodId; }
    public String getUserId() { return userId; }
    public String getProvider() { return provider; }
    public String getProviderPaymentMethodToken() { return providerPaymentMethodToken; }
    public String getCardholderName() { return cardholderName; }
    public String getCardBrand() { return cardBrand; }
    public String getCardLastFour() { return cardLastFour; }
    public int getExpiryMonth() { return expiryMonth; }
    public int getExpiryYear() { return expiryYear; }
    public String getBillingAddressLine1() { return billingAddressLine1; }
    public String getBillingCity() { return billingCity; }
    public String getBillingPostalCode() { return billingPostalCode; }
    public String getBillingCountry() { return billingCountry; }
    public boolean isDefault() { return isDefault; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void updateBillingDetails(int expiryMonth, int expiryYear, String billingAddressLine1,
                                    String billingCity, String billingPostalCode, String billingCountry) {
        this.expiryMonth = expiryMonth;
        this.expiryYear = expiryYear;
        this.billingAddressLine1 = billingAddressLine1;
        this.billingCity = billingCity;
        this.billingPostalCode = billingPostalCode;
        this.billingCountry = billingCountry;
    }

    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }
}
