package com.rentamovil.payment.infrastructure.web.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.rentamovil.payment.domain.port.in.InvoiceUseCases;
import com.rentamovil.payment.domain.port.in.PaymentUseCases;
import com.rentamovil.payment.infrastructure.web.dto.InternalPaymentResponse;

import lombok.RequiredArgsConstructor;

/**
 * Endpoint interno para booking-reservation: le permite embeber el pago/factura de una reserva
 * en su propio detalle (payment-billing README -> "Lo usa booking-reservation para componer el
 * detalle de reserva", inverso de GET /internal/reservations/vehicle-occupancy de ese servicio).
 *
 * <p><b>Sin JWT a propósito</b>: no hay una persona detrás, es un servicio consultando a otro.
 * No se expone fuera de la red interna — el api-gateway no debe enrutar /internal/*.
 */
@RestController
@RequestMapping("/internal/payments")
@RequiredArgsConstructor
public class InternalPaymentController {

    private final PaymentUseCases payments;
    private final InvoiceUseCases invoices;

    /** 204 si la reserva todavía no tiene ningún pago (ni siquiera uno REJECTED). */
    @GetMapping
    public ResponseEntity<InternalPaymentResponse> findLatest(@RequestParam Integer reservationId) {
        return payments.findLatestForReservation(reservationId)
                .map(payment -> {
                    var invoice = invoices.findByReservationIdInternal(reservationId).orElse(null);
                    return ResponseEntity.ok(InternalPaymentResponse.from(payment, invoice));
                })
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
