package com.rentamovil.payment.domain.port.out;

import java.util.List;
import java.util.Optional;

import com.rentamovil.payment.domain.entity.Payment;
import com.rentamovil.payment.domain.enums.PaymentStatus;

public interface PaymentRepositoryPort {

    Payment save(Payment payment);

    Optional<Payment> findById(Integer id);

    /** Admin: la cola de revisión (filtro opcional por status) y la búsqueda por reserva. */
    List<Payment> findAll(Integer reservationId, PaymentStatus status);

    /** El más reciente para esa reserva — lo usa el endpoint interno que consulta booking-reservation. */
    Optional<Payment> findLatestByReservationId(Integer reservationId);
}
