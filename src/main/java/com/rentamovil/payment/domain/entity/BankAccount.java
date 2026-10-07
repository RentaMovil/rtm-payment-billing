package com.rentamovil.payment.domain.entity;

import lombok.Getter;

/**
 * Cuenta bancaria de la empresa mostrada en el checkout. Gestión exclusiva de SUPER_ADMIN
 * (HU-PAYMENT-005) — un ADMIN normal puede revisar pagos pero no tocar esto.
 *
 * <p>Entidad de dominio pura — no usa Spring ni JPA.
 */
@Getter
public class BankAccount {

    private final Integer id;
    private final String bankName;
    private final String accountHolder;
    private final String qrImageUrl;
    private final boolean active;

    private BankAccount(Integer id, String bankName, String accountHolder, String qrImageUrl, boolean active) {
        this.id = id;
        this.bankName = bankName;
        this.accountHolder = accountHolder;
        this.qrImageUrl = qrImageUrl;
        this.active = active;
    }

    /** Nace activa: HU-PAYMENT-005 no tiene un paso de "crear inactiva". */
    public static BankAccount create(String bankName, String accountHolder, String qrImageUrl) {
        return new BankAccount(null, bankName, accountHolder, qrImageUrl, true);
    }

    public static BankAccount restore(Integer id, String bankName, String accountHolder,
                                      String qrImageUrl, boolean active) {
        return new BankAccount(id, bankName, accountHolder, qrImageUrl, active);
    }

    /**
     * Activar/desactivar (HU-PAYMENT-005 Escenario 2). No existe DELETE: una cuenta con
     * historial de pagos no se borra, solo se saca del checkout (data-model.md).
     */
    public BankAccount withActive(boolean newActive) {
        return new BankAccount(id, bankName, accountHolder, qrImageUrl, newActive);
    }
}
