package com.rentamovil.payment.infrastructure.persistence.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.rentamovil.payment.infrastructure.persistence.entity.BankAccountEntity;

/** Acceso Spring Data a payment.bank_account. */
public interface BankAccountJpaRepository extends JpaRepository<BankAccountEntity, Integer> {

    List<BankAccountEntity> findByActiveTrue();
}
