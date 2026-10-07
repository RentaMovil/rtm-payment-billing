package com.rentamovil.payment.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * Cuerpo de POST /bank-accounts (HU-PAYMENT-005). JSON con la URL del QR, no multipart: el
 * navegador ya lo subió a Cloudinary (mismo patrón que la foto de perfil de iam).
 */
@Getter
@Setter
public class CreateBankAccountRequest {

    @NotBlank(message = "obligatorio")
    private String bankName;

    @NotBlank(message = "obligatorio")
    private String accountHolder;

    @NotBlank(message = "falta la imagen del QR")
    private String qrImageUrl;
}
