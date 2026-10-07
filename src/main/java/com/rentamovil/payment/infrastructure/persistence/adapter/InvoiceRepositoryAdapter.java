package com.rentamovil.payment.infrastructure.persistence.adapter;

import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.rentamovil.payment.domain.entity.Invoice;
import com.rentamovil.payment.domain.port.out.InvoiceRepositoryPort;
import com.rentamovil.payment.infrastructure.persistence.repository.InvoiceJpaRepository;

@Component
public class InvoiceRepositoryAdapter implements InvoiceRepositoryPort {

    // 06-data/models.md: invoice_number es VARCHAR(50); 6 dígitos alcanza de sobra para un MVP.
    private static final String NUMBER_FORMAT = "INV-%06d";

    private final InvoiceJpaRepository jpaRepository;

    public InvoiceRepositoryAdapter(InvoiceJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional
    public Invoice save(Invoice invoice) {
        return PersistenceMapper.toDomain(jpaRepository.save(PersistenceMapper.toEntity(invoice)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Invoice> findByReservationId(Integer reservationId) {
        return jpaRepository.findByReservationId(reservationId).map(PersistenceMapper::toDomain);
    }

    @Override
    @Transactional
    public String nextInvoiceNumber() {
        return String.format(NUMBER_FORMAT, jpaRepository.nextSequenceValue());
    }
}
