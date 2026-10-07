package com.rentamovil.payment.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.rentamovil.payment.domain.entity.BankAccount;
import com.rentamovil.payment.domain.port.out.BankAccountRepositoryPort;
import com.rentamovil.payment.infrastructure.persistence.repository.BankAccountJpaRepository;

@Component
public class BankAccountRepositoryAdapter implements BankAccountRepositoryPort {

    private final BankAccountJpaRepository jpaRepository;

    public BankAccountRepositoryAdapter(BankAccountJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional
    public BankAccount save(BankAccount bankAccount) {
        return PersistenceMapper.toDomain(jpaRepository.save(PersistenceMapper.toEntity(bankAccount)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BankAccount> findById(Integer id) {
        return jpaRepository.findById(id).map(PersistenceMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BankAccount> findAll(boolean includeInactive) {
        var entities = includeInactive ? jpaRepository.findAll() : jpaRepository.findByActiveTrue();
        return entities.stream().map(PersistenceMapper::toDomain).toList();
    }
}
