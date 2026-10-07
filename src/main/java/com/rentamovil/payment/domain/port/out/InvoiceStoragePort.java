package com.rentamovil.payment.domain.port.out;

/**
 * Guarda y lee el PDF ya generado. La URL pública que se guarda en invoice.invoice_pdf_url la
 * construye el caso de uso (con el reservationId, para que la descarga pase por el mismo control
 * de propiedad que el resto de la API), no este puerto.
 *
 * <p>Pensado para subir a Cloudinary del lado del servidor (payment-billing-plan), pero hasta que
 * haya un api_secret el adaptador real guarda en una carpeta local y lo sirve el propio servicio
 * (ver {@code infrastructure.pdf}).
 */
public interface InvoiceStoragePort {

    void store(String invoiceNumber, byte[] pdfBytes);

    /** Para servir el PDF por InvoiceController, detrás del mismo control de propiedad que el JSON. */
    byte[] read(String invoiceNumber);
}
