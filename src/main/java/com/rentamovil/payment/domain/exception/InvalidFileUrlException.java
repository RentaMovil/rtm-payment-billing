package com.rentamovil.payment.domain.exception;

// El comprobante o el QR no es una URL de nuestra cuenta de Cloudinary. Responde 400.
public class InvalidFileUrlException extends RuntimeException {

    public InvalidFileUrlException(String message) {
        super(message);
    }
}
