package com.rentamovil.payment.domain.port.out;

import java.util.Optional;

import com.rentamovil.payment.domain.entity.Invoice;

public interface InvoiceRepositoryPort {

    Invoice save(Invoice invoice);

    Optional<Invoice> findByReservationId(Integer reservationId);

    /** NEXT VALUE FOR payment.seq_invoice_number, ya formateado como "INV-000123". */
    String nextInvoiceNumber();
}
