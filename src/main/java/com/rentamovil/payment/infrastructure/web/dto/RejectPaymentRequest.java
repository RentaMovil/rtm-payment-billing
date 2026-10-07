package com.rentamovil.payment.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** Cuerpo de PATCH /payments/{id}/reject (HU-PAYMENT-002, Escenarios 2-3). */
@Getter
@Setter
public class RejectPaymentRequest {

    @NotBlank(message = "obligatorio")
    private String reason;

    @NotNull(message = "obligatorio")
    private String outcome;
}
