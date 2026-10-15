package com.rentamovil.payment.domain.port.in;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import com.rentamovil.payment.domain.entity.AuthenticatedUser;
import com.rentamovil.payment.domain.entity.Payment;
import com.rentamovil.payment.domain.enums.PaymentStatus;

public interface PaymentUseCases {

    /** HU-PAYMENT-001. Valida el monto contra booking-reservation antes de crear el Payment. */
    Payment upload(AuthenticatedUser currentUser, String authorizationHeader, Integer reservationId,
                   Integer bankAccountId, BigDecimal amount, String referenceNumber, String receiptFileUrl);

    /** HU-PAYMENT-002 Escenario 1. Genera la Invoice como parte de la misma operación. */
    Payment approve(AuthenticatedUser currentUser, Integer paymentId);

    /** HU-PAYMENT-002 Escenarios 2-3. outcome: RETRY_ALLOWED o RESERVATION_CANCELLED. */
    Payment reject(AuthenticatedUser currentUser, Integer paymentId, String reason, String outcome);

    /** Detalle de un pago. Exclusivo Admin (payments:read) — el Cliente ve su pago vía la reserva. */
    Payment findById(AuthenticatedUser currentUser, Integer paymentId);

    /** Cola de revisión y búsqueda por reserva. Exclusivo Admin (payments:read). */
    List<Payment> findAll(AuthenticatedUser currentUser, Integer reservationId, PaymentStatus status);

    /**
     * Para GET /internal/payments (booking-reservation componiendo el detalle de una reserva).
     * Sin JWT a propósito, igual que /internal/reservations/vehicle-occupancy de booking: no hay
     * una persona detrás, es un servicio consultando a otro.
     */
    Optional<Payment> findLatestForReservation(Integer reservationId);
}
