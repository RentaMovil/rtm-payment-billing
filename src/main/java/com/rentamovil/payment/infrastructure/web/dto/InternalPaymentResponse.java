package com.rentamovil.payment.infrastructure.web.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.rentamovil.payment.domain.entity.Invoice;
import com.rentamovil.payment.domain.entity.Payment;

import lombok.Getter;

/**
 * Lo que booking-reservation embebe en el detalle de una reserva (GET /internal/payments,
 * ver README -> "Dependencia síncrona" de ese servicio, inversa de esta). Sin JWT: llamada
 * servicio a servicio, igual que /internal/reservations/vehicle-occupancy de booking.
 */
@Getter
public class InternalPaymentResponse {

    private final Integer paymentId;
    private final String status;
    private final BigDecimal amount;
    private final String receiptFileUrl;
    private final LocalDateTime reviewedAt;
    private final String rejectionReason;
    private final String invoiceNumber;
    private final String invoicePdfUrl;

    private InternalPaymentResponse(Integer paymentId, String status, BigDecimal amount, String receiptFileUrl,
                                    LocalDateTime reviewedAt, String rejectionReason, String invoiceNumber,
                                    String invoicePdfUrl) {
        this.paymentId = paymentId;
        this.status = status;
        this.amount = amount;
        this.receiptFileUrl = receiptFileUrl;
        this.reviewedAt = reviewedAt;
        this.rejectionReason = rejectionReason;
        this.invoiceNumber = invoiceNumber;
        this.invoicePdfUrl = invoicePdfUrl;
    }

    public static InternalPaymentResponse from(Payment payment, Invoice invoice) {
        return new InternalPaymentResponse(
                payment.getId(),
                payment.getStatus().name(),
                payment.getAmount(),
                payment.getReceiptFileUrl(),
                payment.getReviewedAt(),
                payment.getRejectionReason(),
                invoice == null ? null : invoice.getInvoiceNumber(),
                invoice == null ? null : invoice.getInvoicePdfUrl());
    }
}
