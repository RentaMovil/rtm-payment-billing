package com.rentamovil.payment.infrastructure.web.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

/**
 * Cuerpo de POST /payments. JSON con la URL del comprobante, no multipart: el navegador ya lo
 * subió a Cloudinary (mismo patrón que la foto de perfil de iam); aquí solo llega la URL.
 */
@Getter
@Setter
public class UploadPaymentRequest {

    @NotNull(message = "obligatorio")
    private Integer reservationId;

    @NotNull(message = "obligatorio")
    private Integer bankAccountId;

    @NotNull(message = "obligatorio")
    @Positive(message = "debe ser mayor a cero")
    private BigDecimal amount;

    private String referenceNumber;

    @NotBlank(message = "falta el archivo del comprobante")
    private String receiptFileUrl;
}
