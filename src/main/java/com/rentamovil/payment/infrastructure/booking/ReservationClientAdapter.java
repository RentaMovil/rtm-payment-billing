package com.rentamovil.payment.infrastructure.booking;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.rentamovil.payment.domain.exception.BusinessRuleException;
import com.rentamovil.payment.domain.exception.ResourceNotFoundException;
import com.rentamovil.payment.domain.port.out.ReservationClientPort;

/**
 * Cliente REST de booking-reservation: lee el monto y el estado de la reserva.
 *
 * <p>Reenvía el JWT de quien hace la petición (decisions.md): si un Cliente pregunta por una
 * reserva ajena, booking responde 404 (no revela que existe) y este servicio hace lo mismo. No
 * hay llamada sin autenticar aquí, a diferencia de fleet-maintenance <-> booking-reservation: sí
 * hay una persona detrás (README -> "Dependencia síncrona").
 */
@Component
public class ReservationClientAdapter implements ReservationClientPort {

    private final RestClient restClient;

    public ReservationClientAdapter(@Value("${app.payment-billing.booking-url}") String bookingUrl,
                                    @Value("${app.payment-billing.booking-timeout}") Duration timeout) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);
        this.restClient = RestClient.builder().baseUrl(bookingUrl).requestFactory(requestFactory).build();
    }

    @Override
    public ReservationSnapshot getReservation(Integer reservationId, String authorizationHeader) {
        return call(reservationId, () -> restClient.get()
                .uri("/reservations/{id}", reservationId)
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .retrieve()
                .body(ReservationApiResponse.class));
    }

    private ReservationSnapshot call(Integer reservationId, Supplier<ReservationApiResponse> request) {
        try {
            ReservationApiResponse response = request.get();
            if (response == null) {
                throw unavailable();
            }
            return new ReservationSnapshot(response.id(), response.totalAmount(), response.status());
        } catch (HttpClientErrorException e) {
            // 401/403 de booking también se traducen a "no existe": este servicio no decide
            // autorización sobre reservas ajenas, solo refleja lo que booking ya decidió.
            if (e.getStatusCode() == HttpStatus.NOT_FOUND || e.getStatusCode() == HttpStatus.FORBIDDEN
                    || e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                throw new ResourceNotFoundException("La reserva " + reservationId + " no existe");
            }
            throw unavailable();
        } catch (RestClientException e) {
            throw unavailable();
        }
    }

    private BusinessRuleException unavailable() {
        return new BusinessRuleException("BOOKING_SERVICE_UNAVAILABLE",
                "El servicio de reservas no está disponible en este momento");
    }

    /** Solo los campos que necesita este servicio de ReservationResponse (booking-reservation.yaml). */
    record ReservationApiResponse(Integer id, BigDecimal totalAmount, String status) {
    }
}
