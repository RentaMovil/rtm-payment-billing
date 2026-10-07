package com.rentamovil.payment.infrastructure.web.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.rentamovil.payment.domain.entity.Payment;

import lombok.Getter;

/** Respuesta de un pago (07-api/contracts/openapi/payment-billing.yaml -> Payment). */
@Getter
public class PaymentResponse {

    private final Integer id;
    private final Integer reservationId;
    private final Integer bankAccountId;
    private final LocalDateTime paymentDate;
    private final BigDecimal amount;
    private final String referenceNumber;
    private final String receiptFileUrl;
    private final String status;
    private final Integer reviewedBy;
    private final LocalDateTime reviewedAt;
    private final String rejectionReason;

    private PaymentResponse(Integer id, Integer reservationId, Integer bankAccountId, LocalDateTime paymentDate,
                            BigDecimal amount, String referenceNumber, String receiptFileUrl, String status,
                            Integer reviewedBy, LocalDateTime reviewedAt, String rejectionReason) {
        this.id = id;
        this.reservationId = reservationId;
        this.bankAccountId = bankAccountId;
        this.paymentDate = paymentDate;
        this.amount = amount;
        this.referenceNumber = referenceNumber;
        this.receiptFileUrl = receiptFileUrl;
        this.status = status;
        this.reviewedBy = reviewedBy;
        this.reviewedAt = reviewedAt;
        this.rejectionReason = rejectionReason;
    }

    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getReservationId(),
                payment.getBankAccountId(),
                payment.getPaymentDate(),
                payment.getAmount(),
                payment.getReferenceNumber(),
                payment.getReceiptFileUrl(),
                payment.getStatus().name(),
                payment.getReviewedBy(),
                payment.getReviewedAt(),
                payment.getRejectionReason());
    }
}
