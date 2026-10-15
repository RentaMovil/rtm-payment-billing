package com.rentamovil.payment.infrastructure.web.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** Cuerpo de PATCH /bank-accounts/{id} (HU-PAYMENT-005, Escenario 2). */
@Getter
@Setter
public class SetBankAccountActiveRequest {

    @NotNull(message = "obligatorio")
    private Boolean isActive;
}
