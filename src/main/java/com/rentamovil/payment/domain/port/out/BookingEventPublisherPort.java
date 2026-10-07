package com.rentamovil.payment.domain.port.out;

/**
 * Envía los eventos de este servicio a booking-reservation (events.md: "Eventos publicados").
 * A diferencia de booking (que reenvía eventos a fleet y también escucha los suyos), este
 * servicio no consume ningún evento — solo necesita publicar, nunca suscribirse.
 */
public interface BookingEventPublisherPort {

    void publish(String eventType, Object payload);
}
