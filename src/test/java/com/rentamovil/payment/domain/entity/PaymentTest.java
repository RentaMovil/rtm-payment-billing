package com.rentamovil.payment.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.rentamovil.payment.domain.enums.PaymentStatus;
import com.rentamovil.payment.domain.exception.BusinessRuleException;

/**
 * Domain tests: no Spring, no database (hexagonal-architecture.md, section 8).
 * Verify the invariants from 02-domain/entities-and-rules.md (Payment.INV-001..INV-003).
 */
class PaymentTest {

    private Payment newPayment() {
        return Payment.create(12, 3, new BigDecimal("400000.00"), "ABC123", "https://res.cloudinary.com/x/receipt.jpg");
    }

    @Test
    @DisplayName("A new payment is born PENDING_REVIEW")
    void bornPendingReview() {
        assertThat(newPayment().getStatus()).isEqualTo(PaymentStatus.PENDING_REVIEW);
    }

    @Test
    @DisplayName("The amount must be greater than zero")
    void amountMustBePositive() {
        assertThatThrownBy(() -> Payment.create(12, 3, BigDecimal.ZERO, "ABC123", "https://x/receipt.jpg"))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("Missing receipt: the payment cannot be created")
    void receiptIsRequired() {
        assertThatThrownBy(() -> Payment.create(12, 3, new BigDecimal("1000"), "ABC123", " "))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("HU-PAYMENT-002 Scenario 1: approving fills reviewedBy/reviewedAt")
    void approveCompletesTheReview() {
        Payment approved = newPayment().approve(99);

        assertThat(approved.getStatus()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(approved.getReviewedBy()).isEqualTo(99);
        assertThat(approved.getReviewedAt()).isNotNull();
    }

    @Test
    @DisplayName("A payment already reviewed cannot be approved again")
    void cannotBeApprovedTwice() {
        Payment approved = newPayment().approve(99);

        assertThatThrownBy(() -> approved.approve(99)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("Rejecting without a reason fails")
    void rejectionWithoutAReasonFails() {
        assertThatThrownBy(() -> newPayment().reject(99, " ")).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("HU-PAYMENT-002 Scenario 2-3: rejecting stores the Administrator's reason")
    void rejectionStoresTheReason() {
        Payment rejected = newPayment().reject(99, "The receipt doesn't match");

        assertThat(rejected.getStatus()).isEqualTo(PaymentStatus.REJECTED);
        assertThat(rejected.getRejectionReason()).isEqualTo("The receipt doesn't match");
    }
}
