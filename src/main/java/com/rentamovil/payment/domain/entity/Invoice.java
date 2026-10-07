package com.rentamovil.payment.domain.entity;

import java.time.LocalDateTime;

import lombok.Getter;

/**
 * Factura generada al aprobar un pago. 1:1 con la reserva: toda la reserva se factura como un
 * solo documento, no por cada payment individual (data-model.md).
 *
 * <p>Entidad de dominio pura — no usa Spring ni JPA.
 */
@Getter
public class Invoice {

    private final Integer id;
    private final Integer reservationId;
    private final String invoiceNumber;
    private final LocalDateTime generatedDate;
    /** NULL justo después de generarse, antes de que termine de subirse el PDF. */
    private final String invoicePdfUrl;

    private Invoice(Integer id, Integer reservationId, String invoiceNumber,
                    LocalDateTime generatedDate, String invoicePdfUrl) {
        this.id = id;
        this.reservationId = reservationId;
        this.invoiceNumber = invoiceNumber;
        this.generatedDate = generatedDate;
        this.invoicePdfUrl = invoicePdfUrl;
    }

    /** invoiceNumber ya viene formateado ("INV-000123") por quien pidió el siguiente de la secuencia. */
    public static Invoice create(Integer reservationId, String invoiceNumber) {
        return new Invoice(null, reservationId, invoiceNumber, LocalDateTime.now(), null);
    }

    public static Invoice restore(Integer id, Integer reservationId, String invoiceNumber,
                                  LocalDateTime generatedDate, String invoicePdfUrl) {
        return new Invoice(id, reservationId, invoiceNumber, generatedDate, invoicePdfUrl);
    }

    /** El PDF se sube después de crear la fila (necesita el invoiceNumber ya asignado). */
    public Invoice withPdfUrl(String newPdfUrl) {
        return new Invoice(id, reservationId, invoiceNumber, generatedDate, newPdfUrl);
    }
}
