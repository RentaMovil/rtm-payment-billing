package com.rentamovil.payment.application.usecase;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.rentamovil.payment.domain.entity.AuthenticatedUser;
import com.rentamovil.payment.domain.entity.Invoice;
import com.rentamovil.payment.domain.entity.Payment;
import com.rentamovil.payment.domain.enums.PaymentStatus;
import com.rentamovil.payment.domain.enums.RejectOutcome;
import com.rentamovil.payment.domain.exception.BusinessRuleException;
import com.rentamovil.payment.domain.exception.ForbiddenException;
import com.rentamovil.payment.domain.exception.InvalidFileUrlException;
import com.rentamovil.payment.domain.exception.ResourceNotFoundException;
import com.rentamovil.payment.domain.port.in.PaymentUseCases;
import com.rentamovil.payment.domain.port.out.AuditPort;
import com.rentamovil.payment.domain.port.out.BookingEventPublisherPort;
import com.rentamovil.payment.domain.port.out.InvoicePdfPort;
import com.rentamovil.payment.domain.port.out.InvoiceRepositoryPort;
import com.rentamovil.payment.domain.port.out.InvoiceStoragePort;
import com.rentamovil.payment.domain.port.out.PaymentRepositoryPort;
import com.rentamovil.payment.domain.port.out.ReservationClientPort;
import com.rentamovil.payment.domain.port.out.ReservationClientPort.ReservationSnapshot;

/**
 * HU-PAYMENT-001 (subir comprobante) y HU-PAYMENT-002 (aprobar/rechazar). La generación de la
 * Invoice vive aquí también: ocurre dentro de la misma operación que aprobar (events.md:
 * "InvoiceGenerated... justo después de PaymentCompleted"), no en un paso separado.
 */
public class PaymentUseCaseImpl implements PaymentUseCases {

    private static final String RESERVATION_STATUS_PENDING_PAYMENT = "PENDING_PAYMENT";

    private final PaymentRepositoryPort paymentRepository;
    private final InvoiceRepositoryPort invoiceRepository;
    private final ReservationClientPort reservationClient;
    private final BookingEventPublisherPort events;
    private final InvoicePdfPort invoicePdf;
    private final InvoiceStoragePort invoiceStorage;
    private final AuditPort audit;
    // Solo se aceptan comprobantes subidos a nuestra cuenta de Cloudinary (mismo patrón que iam
    // con la foto de perfil: ej. https://res.cloudinary.com/<cuenta>/image/upload/)
    private final String allowedFileUrlPrefix;

    public PaymentUseCaseImpl(PaymentRepositoryPort paymentRepository, InvoiceRepositoryPort invoiceRepository,
                              ReservationClientPort reservationClient, BookingEventPublisherPort events,
                              InvoicePdfPort invoicePdf, InvoiceStoragePort invoiceStorage, AuditPort audit,
                              String allowedFileUrlPrefix) {
        this.paymentRepository = paymentRepository;
        this.invoiceRepository = invoiceRepository;
        this.reservationClient = reservationClient;
        this.events = events;
        this.invoicePdf = invoicePdf;
        this.invoiceStorage = invoiceStorage;
        this.audit = audit;
        this.allowedFileUrlPrefix = allowedFileUrlPrefix;
    }

    @Override
    public Payment upload(AuthenticatedUser currentUser, String authorizationHeader, Integer reservationId,
                          Integer bankAccountId, BigDecimal amount, String referenceNumber,
                          String receiptFileUrl) {
        if (!currentUser.hasPermission("payments:create:own")) {
            throw new ForbiddenException("Necesitas permiso para subir un comprobante de pago");
        }
        checkFileUrl(receiptFileUrl);

        // decisions.md: el monto se valida siempre vía API a booking-reservation, nunca por SQL
        // directo. Reenviar el JWT del cliente es lo que hace que la ownership salga gratis: si
        // la reserva no es suya, booking responde 404 y este servicio hace lo mismo.
        ReservationSnapshot reservation = reservationClient.getReservation(reservationId, authorizationHeader);

        if (!RESERVATION_STATUS_PENDING_PAYMENT.equals(reservation.status())) {
            throw new BusinessRuleException("RESERVATION_NOT_PAYABLE",
                    "La reserva no está esperando un pago (status actual: " + reservation.status() + ")");
        }
        // Payment.INV-001: sin pagos parciales, el monto coincide exacto.
        if (amount.compareTo(reservation.totalAmount()) != 0) {
            throw new BusinessRuleException("AMOUNT_MISMATCH",
                    "El monto no coincide con el total de la reserva");
        }

        Payment payment = paymentRepository.save(
                Payment.create(reservationId, bankAccountId, amount, referenceNumber, receiptFileUrl));

        audit.record(currentUser.getUserId(), "PAYMENT_RECEIPT_UPLOADED", "payment", payment.getId(),
                "{\"reservationId\":" + reservationId + "}");
        events.publish("PaymentSubmitted", Map.of(
                "reservationId", reservationId,
                "paymentId", payment.getId()));
        return payment;
    }

    @Override
    public Payment approve(AuthenticatedUser currentUser, Integer paymentId) {
        requirePermission(currentUser, "payments:review");
        Payment approved = paymentRepository.save(find(paymentId).approve(currentUser.getUserId()));

        audit.record(currentUser.getUserId(), "PAYMENT_APPROVED", "payment", paymentId,
                "{\"reservationId\":" + approved.getReservationId() + "}");
        events.publish("PaymentCompleted", Map.of(
                "reservationId", approved.getReservationId(),
                "paymentId", approved.getId(),
                "actorUserId", currentUser.getUserId()));

        generateInvoiceIfMissing(approved);
        return approved;
    }

    @Override
    public Payment reject(AuthenticatedUser currentUser, Integer paymentId, String reason, String outcome) {
        requirePermission(currentUser, "payments:review");
        RejectOutcome parsedOutcome = parseOutcome(outcome);

        Payment rejected = paymentRepository.save(find(paymentId).reject(currentUser.getUserId(), reason));

        audit.record(currentUser.getUserId(), "PAYMENT_REJECTED", "payment", paymentId,
                "{\"reservationId\":" + rejected.getReservationId() + ",\"outcome\":\"" + parsedOutcome + "\"}");
        events.publish("PaymentRejected", Map.of(
                "reservationId", rejected.getReservationId(),
                "paymentId", rejected.getId(),
                "outcome", parsedOutcome.name(),
                "reason", reason));
        return rejected;
    }

    @Override
    public Payment findById(AuthenticatedUser currentUser, Integer paymentId) {
        requirePermission(currentUser, "payments:read");
        return find(paymentId);
    }

    @Override
    public List<Payment> findAll(AuthenticatedUser currentUser, Integer reservationId, PaymentStatus status) {
        requirePermission(currentUser, "payments:read");
        return paymentRepository.findAll(reservationId, status);
    }

    @Override
    public Optional<Payment> findLatestForReservation(Integer reservationId) {
        return paymentRepository.findLatestByReservationId(reservationId);
    }

    /** HU-PAYMENT-003. Idempotente: si ya existe Invoice para esta reserva, no genera otra. */
    private void generateInvoiceIfMissing(Payment approvedPayment) {
        if (invoiceRepository.findByReservationId(approvedPayment.getReservationId()).isPresent()) {
            return;
        }

        Invoice invoice = invoiceRepository.save(
                Invoice.create(approvedPayment.getReservationId(), invoiceRepository.nextInvoiceNumber()));

        byte[] pdfBytes = invoicePdf.generate(invoice, approvedPayment.getReservationId(), approvedPayment.getAmount());
        invoiceStorage.store(invoice.getInvoiceNumber(), pdfBytes);
        Invoice withPdf = invoiceRepository.save(
                invoice.withPdfUrl("/invoices/" + approvedPayment.getReservationId() + "/file"));

        events.publish("InvoiceGenerated", Map.of(
                "reservationId", withPdf.getReservationId(),
                "invoiceId", withPdf.getId(),
                "paymentId", approvedPayment.getId()));
    }

    private void checkFileUrl(String fileUrl) {
        if (!fileUrl.startsWith(allowedFileUrlPrefix)) {
            throw new InvalidFileUrlException("El comprobante debe subirse desde la aplicación");
        }
    }

    private void requirePermission(AuthenticatedUser currentUser, String permission) {
        if (!currentUser.hasPermission(permission)) {
            throw new ForbiddenException("Necesitas el permiso " + permission);
        }
    }

    private RejectOutcome parseOutcome(String outcome) {
        try {
            return RejectOutcome.valueOf(outcome);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BusinessRuleException("UNKNOWN_PAYMENT_OUTCOME", "outcome de rechazo desconocido: " + outcome);
        }
    }

    private Payment find(Integer paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("El pago " + paymentId + " no existe"));
    }
}
