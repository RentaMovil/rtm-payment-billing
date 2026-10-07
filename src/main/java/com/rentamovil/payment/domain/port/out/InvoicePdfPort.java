package com.rentamovil.payment.domain.port.out;

import java.math.BigDecimal;

import com.rentamovil.payment.domain.entity.Invoice;

/** Genera el PDF de una factura. Se renderiza una sola vez, al aprobar el pago (HU-PAYMENT-003). */
public interface InvoicePdfPort {

    byte[] generate(Invoice invoice, Integer reservationId, BigDecimal amount);
}
