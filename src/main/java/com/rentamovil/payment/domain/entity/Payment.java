package com.rentamovil.payment.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.rentamovil.payment.domain.enums.PaymentStatus;
import com.rentamovil.payment.domain.exception.BusinessRuleException;

import lombok.Getter;

/**
 * Un comprobante de transferencia subido por el cliente y su revisión manual por un
 * Administrador. El Aggregate Root de este contexto (data-model.md).
 *
 * <p>Entidad de dominio pura — no usa Spring ni JPA. Las invariantes vienen de
 * {@code 02-domain/entities-and-rules.md} (Payment.INV-001..INV-003).
 */
@Getter
public class Payment {

    private final Integer id;
    private final Integer reservationId;
    private final Integer bankAccountId;
    private final LocalDateTime paymentDate;
    private final BigDecimal amount;
    private final String referenceNumber;
    private final String receiptFileUrl;
    private final PaymentStatus status;
    private final Integer reviewedBy;
    private final LocalDateTime reviewedAt;
    private final String rejectionReason;

    private Payment(Integer id, Integer reservationId, Integer bankAccountId, LocalDateTime paymentDate,
                    BigDecimal amount, String referenceNumber, String receiptFileUrl, PaymentStatus status,
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

    /**
     * Sube un comprobante nuevo. Nace en PENDING_REVIEW: la validación de que amount coincide
     * exacto con reservation.total_amount (INV-001) ya se hizo antes de llegar aquí, contra
     * booking-reservation (no es responsabilidad del dominio de este servicio).
     */
    public static Payment create(Integer reservationId, Integer bankAccountId, BigDecimal amount,
                                 String referenceNumber, String receiptFileUrl) {
        if (amount == null || amount.signum() <= 0) {
            throw new BusinessRuleException("INVALID_AMOUNT", "El monto debe ser mayor a cero");
        }
        if (receiptFileUrl == null || receiptFileUrl.isBlank()) {
            throw new BusinessRuleException("MISSING_RECEIPT", "Falta el archivo del comprobante");
        }
        return new Payment(null, reservationId, bankAccountId, LocalDateTime.now(), amount,
                referenceNumber, receiptFileUrl, PaymentStatus.PENDING_REVIEW, null, null, null);
    }

    public static Payment restore(Integer id, Integer reservationId, Integer bankAccountId,
                                  LocalDateTime paymentDate, BigDecimal amount, String referenceNumber,
                                  String receiptFileUrl, PaymentStatus status, Integer reviewedBy,
                                  LocalDateTime reviewedAt, String rejectionReason) {
        return new Payment(id, reservationId, bankAccountId, paymentDate, amount, referenceNumber,
                receiptFileUrl, status, reviewedBy, reviewedAt, rejectionReason);
    }

    /** HU-PAYMENT-002 Escenario 1: Payment.status -> APPROVED, reviewedBy/reviewedAt se completan. */
    public Payment approve(Integer reviewerUserId) {
        requirePendingReview();
        return new Payment(id, reservationId, bankAccountId, paymentDate, amount, referenceNumber,
                receiptFileUrl, PaymentStatus.APPROVED, reviewerUserId, LocalDateTime.now(), null);
    }

    /** HU-PAYMENT-002 Escenarios 2-3: Payment.status -> REJECTED, con el motivo del Administrador. */
    public Payment reject(Integer reviewerUserId, String reason) {
        requirePendingReview();
        if (reason == null || reason.isBlank()) {
            throw new BusinessRuleException("MISSING_REASON", "El motivo del rechazo es obligatorio");
        }
        return new Payment(id, reservationId, bankAccountId, paymentDate, amount, referenceNumber,
                receiptFileUrl, PaymentStatus.REJECTED, reviewerUserId, LocalDateTime.now(), reason);
    }

    private void requirePendingReview() {
        if (status != PaymentStatus.PENDING_REVIEW) {
            throw new BusinessRuleException("PAYMENT_NOT_PENDING_REVIEW",
                    "El pago ya no está en revisión (status actual: " + status + ")");
        }
    }
}
