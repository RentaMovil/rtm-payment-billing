package com.rentamovil.payment.infrastructure.persistence.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.rentamovil.payment.infrastructure.persistence.entity.InvoiceEntity;

/** Acceso Spring Data a payment.invoice. */
public interface InvoiceJpaRepository extends JpaRepository<InvoiceEntity, Integer> {

    Optional<InvoiceEntity> findByReservationId(Integer reservationId);

    /** payment.seq_invoice_number (01_ddl/03_tables/004): evita derivar el número del IDENTITY. */
    @Query(value = "SELECT NEXT VALUE FOR payment.seq_invoice_number", nativeQuery = true)
    Integer nextSequenceValue();
}
