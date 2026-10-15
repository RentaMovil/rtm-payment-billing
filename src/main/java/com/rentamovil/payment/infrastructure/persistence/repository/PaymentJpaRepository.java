package com.rentamovil.payment.infrastructure.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.rentamovil.payment.infrastructure.persistence.entity.PaymentEntity;

/** Acceso Spring Data a payment.payment. */
public interface PaymentJpaRepository extends JpaRepository<PaymentEntity, Integer> {

    List<PaymentEntity> findAllByOrderByPaymentDateDesc();

    List<PaymentEntity> findByReservationIdOrderByPaymentDateDesc(Integer reservationId);

    List<PaymentEntity> findByStatusOrderByPaymentDateAsc(String status);

    List<PaymentEntity> findByReservationIdAndStatusOrderByPaymentDateAsc(Integer reservationId, String status);

    Optional<PaymentEntity> findFirstByReservationIdOrderByPaymentDateDesc(Integer reservationId);
}
