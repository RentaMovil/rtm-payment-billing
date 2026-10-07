package com.rentamovil.payment.application.usecase;

import java.util.Optional;

import com.rentamovil.payment.domain.entity.AuthenticatedUser;
import com.rentamovil.payment.domain.entity.Invoice;
import com.rentamovil.payment.domain.exception.ForbiddenException;
import com.rentamovil.payment.domain.exception.ResourceNotFoundException;
import com.rentamovil.payment.domain.port.in.InvoiceUseCases;
import com.rentamovil.payment.domain.port.out.InvoiceRepositoryPort;
import com.rentamovil.payment.domain.port.out.InvoiceStoragePort;
import com.rentamovil.payment.domain.port.out.ReservationClientPort;

/** HU-PAYMENT-004: ver y descargar la factura ya generada (nunca se regenera). */
public class InvoiceUseCaseImpl implements InvoiceUseCases {

    private final InvoiceRepositoryPort invoiceRepository;
    private final InvoiceStoragePort invoiceStorage;
    private final ReservationClientPort reservationClient;

    public InvoiceUseCaseImpl(InvoiceRepositoryPort invoiceRepository, InvoiceStoragePort invoiceStorage,
                              ReservationClientPort reservationClient) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceStorage = invoiceStorage;
        this.reservationClient = reservationClient;
    }

    @Override
    public Invoice findByReservationId(AuthenticatedUser currentUser, String authorizationHeader,
                                       Integer reservationId) {
        checkAccess(currentUser, authorizationHeader, reservationId);
        return find(reservationId);
    }

    @Override
    public byte[] downloadPdf(AuthenticatedUser currentUser, String authorizationHeader, Integer reservationId) {
        checkAccess(currentUser, authorizationHeader, reservationId);
        Invoice invoice = find(reservationId);
        if (invoice.getInvoicePdfUrl() == null) {
            throw new ResourceNotFoundException("El PDF de esta factura todavía no está listo");
        }
        return invoiceStorage.read(invoice.getInvoiceNumber());
    }

    @Override
    public Optional<Invoice> findByReservationIdInternal(Integer reservationId) {
        return invoiceRepository.findByReservationId(reservationId);
    }

    /**
     * invoices:read (Admin) ve cualquiera; invoices:read:own necesita además ser el dueño de la
     * reserva, verificado contra booking-reservation (mismo patrón que al pagar: booking decide
     * la propiedad, aquí solo se refleja).
     */
    private void checkAccess(AuthenticatedUser currentUser, String authorizationHeader, Integer reservationId) {
        if (currentUser.hasPermission("invoices:read")) {
            return;
        }
        if (!currentUser.hasPermission("invoices:read:own")) {
            throw new ForbiddenException("Necesitas permiso para ver facturas");
        }
        // Lanza ResourceNotFoundException (404) si la reserva no es del que pregunta.
        reservationClient.getReservation(reservationId, authorizationHeader);
    }

    private Invoice find(Integer reservationId) {
        return invoiceRepository.findByReservationId(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Todavía no hay factura para la reserva " + reservationId));
    }
}
