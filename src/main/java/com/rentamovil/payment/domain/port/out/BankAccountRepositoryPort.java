package com.rentamovil.payment.domain.port.out;

import java.util.List;
import java.util.Optional;

import com.rentamovil.payment.domain.entity.BankAccount;

public interface BankAccountRepositoryPort {

    BankAccount save(BankAccount bankAccount);

    Optional<BankAccount> findById(Integer id);

    List<BankAccount> findAll(boolean includeInactive);
}
