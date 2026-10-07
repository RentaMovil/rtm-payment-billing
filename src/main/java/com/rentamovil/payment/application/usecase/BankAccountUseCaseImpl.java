package com.rentamovil.payment.application.usecase;

import java.util.List;

import com.rentamovil.payment.domain.entity.AuthenticatedUser;
import com.rentamovil.payment.domain.entity.BankAccount;
import com.rentamovil.payment.domain.exception.ForbiddenException;
import com.rentamovil.payment.domain.exception.InvalidFileUrlException;
import com.rentamovil.payment.domain.exception.ResourceNotFoundException;
import com.rentamovil.payment.domain.port.in.BankAccountUseCases;
import com.rentamovil.payment.domain.port.out.AuditPort;
import com.rentamovil.payment.domain.port.out.BankAccountRepositoryPort;

/** HU-PAYMENT-005: cuentas bancarias de la empresa. Exclusivo bank-accounts:manage (SUPER_ADMIN). */
public class BankAccountUseCaseImpl implements BankAccountUseCases {

    private static final String MANAGE_PERMISSION = "bank-accounts:manage";

    private final BankAccountRepositoryPort bankAccountRepository;
    private final AuditPort audit;
    // Mismo patrón que el comprobante de pago: solo URLs de nuestra cuenta de Cloudinary.
    private final String allowedFileUrlPrefix;

    public BankAccountUseCaseImpl(BankAccountRepositoryPort bankAccountRepository, AuditPort audit,
                                  String allowedFileUrlPrefix) {
        this.bankAccountRepository = bankAccountRepository;
        this.audit = audit;
        this.allowedFileUrlPrefix = allowedFileUrlPrefix;
    }

    @Override
    public List<BankAccount> findAll(AuthenticatedUser currentUser, boolean includeInactive) {
        if (includeInactive && !currentUser.hasPermission(MANAGE_PERMISSION)) {
            throw new ForbiddenException("Solo quien administra las cuentas bancarias puede ver las inactivas");
        }
        return bankAccountRepository.findAll(includeInactive);
    }

    @Override
    public BankAccount create(AuthenticatedUser currentUser, String bankName, String accountHolder,
                              String qrImageUrl) {
        requireManage(currentUser);
        if (!qrImageUrl.startsWith(allowedFileUrlPrefix)) {
            throw new InvalidFileUrlException("El QR debe subirse desde la aplicación");
        }

        BankAccount created = bankAccountRepository.save(BankAccount.create(bankName, accountHolder, qrImageUrl));
        audit.record(currentUser.getUserId(), "BANK_ACCOUNT_CREATED", "bank_account", created.getId(), null);
        return created;
    }

    @Override
    public BankAccount setActive(AuthenticatedUser currentUser, Integer id, boolean active) {
        requireManage(currentUser);
        BankAccount bankAccount = bankAccountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("La cuenta bancaria " + id + " no existe"));

        BankAccount updated = bankAccountRepository.save(bankAccount.withActive(active));
        audit.record(currentUser.getUserId(), active ? "BANK_ACCOUNT_ACTIVATED" : "BANK_ACCOUNT_DEACTIVATED",
                "bank_account", id, null);
        return updated;
    }

    private void requireManage(AuthenticatedUser currentUser) {
        if (!currentUser.hasPermission(MANAGE_PERMISSION)) {
            throw new ForbiddenException("Necesitas permiso para administrar cuentas bancarias");
        }
    }
}
