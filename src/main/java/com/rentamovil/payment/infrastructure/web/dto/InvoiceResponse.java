package com.rentamovil.payment.infrastructure.web.dto;

import java.time.LocalDateTime;

import com.rentamovil.payment.domain.entity.Invoice;

import lombok.Getter;

/** Respuesta de una factura (07-api/contracts/openapi/payment-billing.yaml -> Invoice). */
@Getter
public class InvoiceResponse {

    private final Integer id;
    private final Integer reservationId;
    private final String invoiceNumber;
    private final LocalDateTime generatedDate;
    private final String invoicePdfUrl;

    private InvoiceResponse(Integer id, Integer reservationId, String invoiceNumber,
                            LocalDateTime generatedDate, String invoicePdfUrl) {
        this.id = id;
        this.reservationId = reservationId;
        this.invoiceNumber = invoiceNumber;
        this.generatedDate = generatedDate;
        this.invoicePdfUrl = invoicePdfUrl;
    }

    public static InvoiceResponse from(Invoice invoice) {
        return new InvoiceResponse(invoice.getId(), invoice.getReservationId(), invoice.getInvoiceNumber(),
                invoice.getGeneratedDate(), invoice.getInvoicePdfUrl());
    }
}
