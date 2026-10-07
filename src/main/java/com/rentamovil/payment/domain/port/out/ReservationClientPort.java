package com.rentamovil.payment.domain.port.out;

import java.math.BigDecimal;

/**
 * Lee la reserva desde booking-reservation (decisions.md: "verificación del monto vía API,
 * nunca por SQL directo", aunque la FK física exista en la misma base).
 *
 * <p>Reenvía el JWT de quien hace la petición: booking-reservation es quien decide si el
 * Cliente es dueño de esa reserva o si quien pregunta es un Administrador (404 si no).
 */
public interface ReservationClientPort {

    ReservationSnapshot getReservation(Integer reservationId, String authorizationHeader);

    record ReservationSnapshot(Integer id, BigDecimal totalAmount, String status) {
    }
}
