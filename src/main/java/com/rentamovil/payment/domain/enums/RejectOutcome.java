package com.rentamovil.payment.domain.enums;

/** Qué le pasa a la reserva cuando se rechaza un pago (HU-PAYMENT-002, Escenarios 2-3). */
public enum RejectOutcome {
    /** El cliente puede volver a subir un comprobante. */
    RETRY_ALLOWED,
    /** El Administrador se rindió con esta reserva: se cancela. */
    RESERVATION_CANCELLED
}
