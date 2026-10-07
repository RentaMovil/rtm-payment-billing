package com.rentamovil.payment.domain.port.in;

import java.util.List;

import com.rentamovil.payment.domain.entity.AuthenticatedUser;
import com.rentamovil.payment.domain.entity.BankAccount;

public interface BankAccountUseCases {

    /** includeInactive solo lo puede pedir quien tiene bank-accounts:manage (SUPER_ADMIN). */
    List<BankAccount> findAll(AuthenticatedUser currentUser, boolean includeInactive);

    /** HU-PAYMENT-005. Exclusivo bank-accounts:manage. Nace activa. */
    BankAccount create(AuthenticatedUser currentUser, String bankName, String accountHolder, String qrImageUrl);

    /** HU-PAYMENT-005 Escenario 2. Exclusivo bank-accounts:manage. Sin DELETE. */
    BankAccount setActive(AuthenticatedUser currentUser, Integer id, boolean active);
}
