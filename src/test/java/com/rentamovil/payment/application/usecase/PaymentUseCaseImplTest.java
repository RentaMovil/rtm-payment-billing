package com.rentamovil.payment.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.rentamovil.payment.domain.entity.AuthenticatedUser;
import com.rentamovil.payment.domain.entity.Invoice;
import com.rentamovil.payment.domain.entity.Payment;
import com.rentamovil.payment.domain.exception.BusinessRuleException;
import com.rentamovil.payment.domain.exception.ForbiddenException;
import com.rentamovil.payment.domain.port.out.AuditPort;
import com.rentamovil.payment.domain.port.out.BookingEventPublisherPort;
import com.rentamovil.payment.domain.port.out.InvoicePdfPort;
import com.rentamovil.payment.domain.port.out.InvoiceRepositoryPort;
import com.rentamovil.payment.domain.port.out.InvoiceStoragePort;
import com.rentamovil.payment.domain.port.out.PaymentRepositoryPort;
import com.rentamovil.payment.domain.port.out.ReservationClientPort;
import com.rentamovil.payment.domain.port.out.ReservationClientPort.ReservationSnapshot;

/** HU-PAYMENT-001 (subir) y HU-PAYMENT-002 (revisión) + la generación de la Invoice al aprobar. */
@ExtendWith(MockitoExtension.class)
class PaymentUseCaseImplTest {

    private static final Integer RESERVATION_ID = 12;
    private static final String AUTH_HEADER = "Bearer client-token";
    private static final String ALLOWED_PREFIX = "https://res.cloudinary.com/dz6ohgjub/image/upload/";
    private static final String RECEIPT_URL = ALLOWED_PREFIX + "receipt.jpg";

    @Mock
    private PaymentRepositoryPort paymentRepository;
    @Mock
    private InvoiceRepositoryPort invoiceRepository;
    @Mock
    private ReservationClientPort reservationClient;
    @Mock
    private BookingEventPublisherPort events;
    @Mock
    private InvoicePdfPort invoicePdf;
    @Mock
    private InvoiceStoragePort invoiceStorage;
    @Mock
    private AuditPort audit;

    private PaymentUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new PaymentUseCaseImpl(paymentRepository, invoiceRepository, reservationClient, events,
                invoicePdf, invoiceStorage, audit, ALLOWED_PREFIX);
        // save() simula el IDENTITY de la base: sin esto payment.getId() queda null y Map.of()
        // (los payloads de los eventos) revienta con NullPointerException, como pasaría también
        // en producción si alguna vez save() no devolviera el id generado.
        lenient().when(paymentRepository.save(any())).thenAnswer(invocation -> {
            Payment arg = invocation.getArgument(0);
            return arg.getId() != null ? arg : Payment.restore(40, arg.getReservationId(), arg.getBankAccountId(),
                    arg.getPaymentDate(), arg.getAmount(), arg.getReferenceNumber(), arg.getReceiptFileUrl(),
                    arg.getStatus(), arg.getReviewedBy(), arg.getReviewedAt(), arg.getRejectionReason());
        });
    }

    private AuthenticatedUser client() {
        return new AuthenticatedUser(6004, 6010, "CLIENT", List.of("payments:create:own"));
    }

    private AuthenticatedUser admin() {
        return new AuthenticatedUser(1, 1, "ADMIN", List.of("payments:review", "payments:read"));
    }

    @Test
    @DisplayName("HU-PAYMENT-001 Escenario 1: monto exacto crea el pago y avisa a booking")
    void subidaExitosaPublicaPaymentSubmitted() {
        when(reservationClient.getReservation(RESERVATION_ID, AUTH_HEADER))
                .thenReturn(new ReservationSnapshot(RESERVATION_ID, new BigDecimal("250000.00"), "PENDING_PAYMENT"));

        Payment payment = useCase.upload(client(), AUTH_HEADER, RESERVATION_ID, 3,
                new BigDecimal("250000.00"), "ABC123", RECEIPT_URL);

        assertThat(payment.getReservationId()).isEqualTo(RESERVATION_ID);
        verify(events).publish(eq("PaymentSubmitted"), any());
    }

    @Test
    @DisplayName("HU-PAYMENT-001 Escenario 2: el monto no coincide, responde AMOUNT_MISMATCH")
    void montoDistintoFalla() {
        when(reservationClient.getReservation(RESERVATION_ID, AUTH_HEADER))
                .thenReturn(new ReservationSnapshot(RESERVATION_ID, new BigDecimal("250000.00"), "PENDING_PAYMENT"));

        assertThatThrownBy(() -> useCase.upload(client(), AUTH_HEADER, RESERVATION_ID, 3,
                new BigDecimal("200000.00"), "ABC123", RECEIPT_URL))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(e -> assertThat(((BusinessRuleException) e).getCode()).isEqualTo("AMOUNT_MISMATCH"));
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Una reserva que ya no está PENDING_PAYMENT no admite un pago nuevo")
    void reservaNoPagableFalla() {
        when(reservationClient.getReservation(RESERVATION_ID, AUTH_HEADER))
                .thenReturn(new ReservationSnapshot(RESERVATION_ID, new BigDecimal("250000.00"), "CONFIRMED"));

        assertThatThrownBy(() -> useCase.upload(client(), AUTH_HEADER, RESERVATION_ID, 3,
                new BigDecimal("250000.00"), "ABC123", RECEIPT_URL))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("Un comprobante de otra cuenta de Cloudinary se rechaza antes de llamar a booking")
    void comprobanteDeOtraCuentaSeRechaza() {
        assertThatThrownBy(() -> useCase.upload(client(), AUTH_HEADER, RESERVATION_ID, 3,
                new BigDecimal("250000.00"), "ABC123", "https://res.cloudinary.com/otra-cuenta/x.jpg"))
                .isInstanceOf(com.rentamovil.payment.domain.exception.InvalidFileUrlException.class);
        verify(reservationClient, never()).getReservation(any(), anyString());
    }

    @Test
    @DisplayName("Sin el permiso payments:create:own no se puede subir un comprobante")
    void sinPermisoFalla() {
        AuthenticatedUser sinPermiso = new AuthenticatedUser(1, 1, "CLIENT", List.of());

        assertThatThrownBy(() -> useCase.upload(sinPermiso, AUTH_HEADER, RESERVATION_ID, 3,
                new BigDecimal("250000.00"), "ABC123", RECEIPT_URL))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("HU-PAYMENT-002 Escenario 1: aprobar genera la factura una sola vez")
    void aprobarGeneraFacturaUnaVez() {
        Payment pending = Payment.restore(40, RESERVATION_ID, 3, java.time.LocalDateTime.now(),
                new BigDecimal("250000.00"), "ABC123", RECEIPT_URL,
                com.rentamovil.payment.domain.enums.PaymentStatus.PENDING_REVIEW, null, null, null);
        when(paymentRepository.findById(40)).thenReturn(Optional.of(pending));
        when(invoiceRepository.findByReservationId(RESERVATION_ID)).thenReturn(Optional.empty());
        when(invoiceRepository.nextInvoiceNumber()).thenReturn("INV-000001");
        // Igual que paymentRepository.save(): simula el IDENTITY, invoice.getId() no puede ser null.
        when(invoiceRepository.save(any())).thenAnswer(invocation -> {
            Invoice arg = invocation.getArgument(0);
            return arg.getId() != null ? arg : Invoice.restore(1, arg.getReservationId(), arg.getInvoiceNumber(),
                    arg.getGeneratedDate(), arg.getInvoicePdfUrl());
        });
        when(invoicePdf.generate(any(), eq(RESERVATION_ID), any())).thenReturn(new byte[] {1, 2, 3});

        useCase.approve(admin(), 40);

        verify(invoiceStorage).store(eq("INV-000001"), any());
        verify(events).publish(eq("PaymentCompleted"), any());
        verify(events).publish(eq("InvoiceGenerated"), any());
    }

    @Test
    @DisplayName("Si ya existe factura para la reserva, aprobar no genera una segunda")
    void noDuplicaFacturaSiYaExiste() {
        Payment pending = Payment.restore(40, RESERVATION_ID, 3, java.time.LocalDateTime.now(),
                new BigDecimal("250000.00"), "ABC123", RECEIPT_URL,
                com.rentamovil.payment.domain.enums.PaymentStatus.PENDING_REVIEW, null, null, null);
        when(paymentRepository.findById(40)).thenReturn(Optional.of(pending));
        when(invoiceRepository.findByReservationId(RESERVATION_ID))
                .thenReturn(Optional.of(Invoice.restore(1, RESERVATION_ID, "INV-000001",
                        java.time.LocalDateTime.now(), "/invoices/" + RESERVATION_ID + "/file")));

        useCase.approve(admin(), 40);

        verify(invoiceRepository, never()).nextInvoiceNumber();
        verify(events, never()).publish(eq("InvoiceGenerated"), any());
    }
}
