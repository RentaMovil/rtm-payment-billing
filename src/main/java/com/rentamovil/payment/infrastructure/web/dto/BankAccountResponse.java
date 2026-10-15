package com.rentamovil.payment.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.rentamovil.payment.domain.entity.BankAccount;

import lombok.AccessLevel;
import lombok.Getter;

/** Respuesta de una cuenta bancaria (07-api/contracts/openapi/payment-billing.yaml -> BankAccount). */
@Getter
public class BankAccountResponse {

    private final Integer id;
    private final String bankName;
    private final String accountHolder;
    private final String qrImageUrl;
    // Sin getter de Lombok: isActive() "activa" y active() "isActive" no coinciden en el nombre
    // implícito que usa Jackson, y acaba serializando las dos — de ahí el getter manual.
    @Getter(AccessLevel.NONE)
    private final boolean active;

    private BankAccountResponse(Integer id, String bankName, String accountHolder, String qrImageUrl,
                                boolean active) {
        this.id = id;
        this.bankName = bankName;
        this.accountHolder = accountHolder;
        this.qrImageUrl = qrImageUrl;
        this.active = active;
    }

    @JsonProperty("isActive")
    public boolean isActive() {
        return active;
    }

    public static BankAccountResponse from(BankAccount bankAccount) {
        return new BankAccountResponse(bankAccount.getId(), bankAccount.getBankName(),
                bankAccount.getAccountHolder(), bankAccount.getQrImageUrl(), bankAccount.isActive());
    }
}
