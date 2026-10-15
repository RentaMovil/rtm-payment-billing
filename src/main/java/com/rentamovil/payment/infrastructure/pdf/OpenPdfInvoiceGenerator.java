package com.rentamovil.payment.infrastructure.pdf;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Component;

import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import com.rentamovil.payment.domain.entity.Invoice;
import com.rentamovil.payment.domain.exception.BusinessRuleException;
import com.rentamovil.payment.domain.port.out.InvoicePdfPort;

/**
 * Factura mínima en PDF: lo que pide HU-PAYMENT-003 (que exista un comprobante descargable), sin
 * maquetación de boleta fiscal real porque no hay una HU que la exija.
 */
@Component
public class OpenPdfInvoiceGenerator implements InvoicePdfPort {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public byte[] generate(Invoice invoice, Integer reservationId, BigDecimal amount) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, out);
            document.open();

            Font title = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Font body = FontFactory.getFont(FontFactory.HELVETICA, 12);

            document.add(new Paragraph("RentaMovil", title));
            document.add(new Paragraph("Factura " + invoice.getInvoiceNumber(), title));
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Reserva: #" + reservationId, body));
            document.add(new Paragraph("Fecha de emisión: " + invoice.getGeneratedDate().format(DATE_FORMAT), body));
            document.add(new Paragraph("Total pagado: $" + amount.toPlainString(), body));

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new BusinessRuleException("INVOICE_PDF_GENERATION_FAILED",
                    "No se pudo generar el PDF de la factura");
        }
    }
}
