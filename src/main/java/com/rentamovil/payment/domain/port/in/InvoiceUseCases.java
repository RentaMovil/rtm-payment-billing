package com.rentamovil.payment.domain.port.in;

import java.util.Optional;

import com.rentamovil.payment.domain.entity.AuthenticatedUser;
import com.rentamovil.payment.domain.entity.Invoice;

public interface InvoiceUseCases {

    /**
     * HU-PAYMENT-004. El dueño de la reserva (invoices:read:own) o un Admin (invoices:read)
     * pueden verla; la propiedad se verifica contra booking-reservation igual que al pagar.
     */
    Invoice findByReservationId(AuthenticatedUser currentUser, String authorizationHeader, Integer reservationId);

    /** El PDF ya generado (nunca se regenera). Mismo control de acceso que findByReservationId. */
    byte[] downloadPdf(AuthenticatedUser currentUser, String authorizationHeader, Integer reservationId);

    /** Para GET /internal/payments. Sin JWT a propósito (ver PaymentUseCases.findLatestForReservation). */
    Optional<Invoice> findByReservationIdInternal(Integer reservationId);
}
