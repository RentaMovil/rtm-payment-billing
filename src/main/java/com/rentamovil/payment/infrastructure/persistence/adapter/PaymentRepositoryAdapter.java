package com.rentamovil.payment.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.rentamovil.payment.domain.entity.Payment;
import com.rentamovil.payment.domain.enums.PaymentStatus;
import com.rentamovil.payment.domain.port.out.PaymentRepositoryPort;
import com.rentamovil.payment.infrastructure.persistence.repository.PaymentJpaRepository;

@Component
public class PaymentRepositoryAdapter implements PaymentRepositoryPort {

    private final PaymentJpaRepository jpaRepository;

    public PaymentRepositoryAdapter(PaymentJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional
    public Payment save(Payment payment) {
        return PersistenceMapper.toDomain(jpaRepository.save(PersistenceMapper.toEntity(payment)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Payment> findById(Integer id) {
        return jpaRepository.findById(id).map(PersistenceMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Payment> findAll(Integer reservationId, PaymentStatus status) {
        List<com.rentamovil.payment.infrastructure.persistence.entity.PaymentEntity> entities;
        if (reservationId != null && status != null) {
            entities = jpaRepository.findByReservationIdAndStatusOrderByPaymentDateAsc(reservationId, status.name());
        } else if (reservationId != null) {
            entities = jpaRepository.findByReservationIdOrderByPaymentDateDesc(reservationId);
        } else if (status != null) {
            entities = jpaRepository.findByStatusOrderByPaymentDateAsc(status.name());
        } else {
            entities = jpaRepository.findAllByOrderByPaymentDateDesc();
        }
        return entities.stream().map(PersistenceMapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Payment> findLatestByReservationId(Integer reservationId) {
        return jpaRepository.findFirstByReservationIdOrderByPaymentDateDesc(reservationId)
                .map(PersistenceMapper::toDomain);
    }
}
