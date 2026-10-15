package com.rentamovil.payment.infrastructure.persistence.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Mapeo de payment.invoice. La tabla la crea rtm-payment-billing-db; Hibernate solo valida. */
@Entity
@Table(name = "invoice")
@Getter
@Setter
@NoArgsConstructor
public class InvoiceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "invoice_id")
    private Integer id;

    /** FK a booking.reservation (rtm-booking-db). 1:1 (uq_invoice_reservation_id). */
    @Column(name = "reservation_id", nullable = false)
    private Integer reservationId;

    @Column(name = "invoice_number", nullable = false, length = 50)
    private String invoiceNumber;

    @Column(name = "generated_date", nullable = false)
    private LocalDateTime generatedDate;

    /** NULL justo después de generarse, antes de que termine de subirse el PDF. */
    @Column(name = "invoice_pdf_url", length = 255)
    private String invoicePdfUrl;
}
