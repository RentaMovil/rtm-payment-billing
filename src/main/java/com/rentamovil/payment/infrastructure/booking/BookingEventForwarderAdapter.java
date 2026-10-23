package com.rentamovil.payment.infrastructure.booking;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.rentamovil.payment.domain.port.out.BookingEventPublisherPort;

/**
 * Envía PaymentSubmitted, PaymentCompleted, PaymentRejected e InvoiceGenerated a
 * booking-reservation (POST /internal/events), que con eso mueve Reservation.status.
 *
 * <p>Mismo sobre que usan todos los servicios: eventId, eventType, occurredAt, payload. booking
 * es idempotente (events.md: "at-least-once"), así que reenviar un evento no hace daño.
 *
 * <p>Si booking está caído el evento se pierde y queda en el log con su eventId: no hay outbox
 * todavía (mismo nivel que fleet-maintenance <-> booking-reservation, FleetEventForwarder). La
 * operación que ya tuvo éxito en este servicio (el pago quedó guardado) no se revierte por eso.
 */
@Component
public class BookingEventForwarderAdapter implements BookingEventPublisherPort {

    private static final Logger log = LoggerFactory.getLogger(BookingEventForwarderAdapter.class);

    private final RestClient restClient;

    public BookingEventForwarderAdapter(@Value("${app.payment-billing.booking-url}") String bookingUrl,
                                        @Value("${app.payment-billing.booking-timeout}") Duration timeout,
                                        @Value("${app.internal.api-key}") String internalApiKey) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);
        this.restClient = RestClient.builder().baseUrl(bookingUrl).requestFactory(requestFactory)
                .defaultHeader("X-Internal-Key", internalApiKey).build();
    }

    @Override
    public void publish(String eventType, Object payload) {
        String eventId = UUID.randomUUID().toString();
        Map<String, Object> envelope = Map.of(
                "eventId", eventId,
                "eventType", eventType,
                "occurredAt", Instant.now().toString(),
                "payload", payload);
        try {
            restClient.post()
                    .uri("/internal/events")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(envelope)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Evento {} enviado a booking-reservation (eventId={})", eventType, eventId);
        } catch (RestClientException e) {
            log.error("No se pudo enviar {} a booking-reservation (eventId={}, payload={})",
                    eventType, eventId, payload, e);
        }
    }
}
