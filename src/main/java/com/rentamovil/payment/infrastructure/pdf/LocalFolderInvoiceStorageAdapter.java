package com.rentamovil.payment.infrastructure.pdf;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.rentamovil.payment.domain.exception.BusinessRuleException;
import com.rentamovil.payment.domain.port.out.InvoiceStoragePort;

/**
 * Guarda y lee el PDF en una carpeta local; lo sirve InvoiceController detrás del mismo control
 * de propiedad que el resto de la API (GET /invoices/{reservationId}/file).
 *
 * <p>payment-billing-plan: el destino real es Cloudinary del lado del servidor (igual que la
 * foto de perfil, pero con el api_secret porque aquí sube el backend, no el navegador). Mientras
 * no haya ese api_secret, este adaptador es el que implementa InvoiceStoragePort — cambiar a
 * Cloudinary después es sustituir esta clase, el dominio no se entera.
 */
@Component
public class LocalFolderInvoiceStorageAdapter implements InvoiceStoragePort {

    private final Path storageDir;

    public LocalFolderInvoiceStorageAdapter(
            @Value("${app.payment-billing.invoice-storage.dir}") String storageDir) {
        this.storageDir = Path.of(storageDir);
        try {
            Files.createDirectories(this.storageDir);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo crear la carpeta de facturas: " + storageDir, e);
        }
    }

    @Override
    public void store(String invoiceNumber, byte[] pdfBytes) {
        try {
            Files.write(storageDir.resolve(fileName(invoiceNumber)), pdfBytes);
        } catch (IOException e) {
            throw new BusinessRuleException("INVOICE_STORAGE_FAILED",
                    "No se pudo guardar el PDF de la factura");
        }
    }

    @Override
    public byte[] read(String invoiceNumber) {
        try {
            return Files.readAllBytes(storageDir.resolve(fileName(invoiceNumber)));
        } catch (IOException e) {
            throw new com.rentamovil.payment.domain.exception.ResourceNotFoundException(
                    "El PDF de la factura " + invoiceNumber + " no existe");
        }
    }

    private String fileName(String invoiceNumber) {
        return invoiceNumber + ".pdf";
    }
}
