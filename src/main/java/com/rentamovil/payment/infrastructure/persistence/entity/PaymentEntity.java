package com.rentamovil.payment.infrastructure.persistence.entity;

import java.math.BigDecimal;
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

/** Mapeo de payment.payment. La tabla la crea rtm-payment-billing-db; Hibernate solo valida. */
@Entity
@Table(name = "payment")
@Getter
@Setter
@NoArgsConstructor
public class PaymentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Integer id;

    /** FK a booking.reservation (rtm-booking-db). Sin relación JPA: reservation es de otro servicio. */
    @Column(name = "reservation_id", nullable = false)
    private Integer reservationId;

    @Column(name = "bank_account_id", nullable = false)
    private Integer bankAccountId;

    @Column(name = "payment_date", nullable = false)
    private LocalDateTime paymentDate;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "reference_number", length = 100)
    private String referenceNumber;

    @Column(name = "receipt_file_url", nullable = false, length = 255)
    private String receiptFileUrl;

    // String, no el enum: el estado se guarda y se lee como texto (el CHECK de la tabla lo valida
    // igual). El mapper convierte con getStatus().name() y PaymentStatus.valueOf().
    @Column(name = "status", nullable = false, length = 30)
    private String status;

    /** FK a iam.[user] (rtm-iam-db). NULL mientras status = PENDING_REVIEW. */
    @Column(name = "reviewed_by")
    private Integer reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "rejection_reason", length = 255)
    private String rejectionReason;
}
