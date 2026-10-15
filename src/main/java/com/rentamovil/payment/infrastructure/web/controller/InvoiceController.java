package com.rentamovil.payment.infrastructure.web.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rentamovil.payment.domain.entity.AuthenticatedUser;
import com.rentamovil.payment.domain.port.in.InvoiceUseCases;
import com.rentamovil.payment.infrastructure.web.auth.CurrentUser;
import com.rentamovil.payment.infrastructure.web.dto.InvoiceResponse;

import lombok.RequiredArgsConstructor;

/** HU-PAYMENT-003 (generación, automática al aprobar) y HU-PAYMENT-004 (ver/descargar). */
@RestController
@RequestMapping("/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceUseCases invoices;

    /** HU-PAYMENT-004: sirve el mismo PDF ya generado, nunca lo regenera en cada consulta. */
    @GetMapping("/{reservationId}")
    public InvoiceResponse findByReservationId(@CurrentUser AuthenticatedUser currentUser,
                                               @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
                                               @PathVariable Integer reservationId) {
        return InvoiceResponse.from(invoices.findByReservationId(currentUser, authorization, reservationId));
    }

    /** Los bytes del PDF — misma protección de propiedad que el endpoint anterior. */
    @GetMapping("/{reservationId}/file")
    public ResponseEntity<byte[]> downloadPdf(@CurrentUser AuthenticatedUser currentUser,
                                              @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
                                              @PathVariable Integer reservationId) {
        byte[] pdf = invoices.downloadPdf(currentUser, authorization, reservationId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"factura-" + reservationId + ".pdf\"")
                .body(pdf);
    }
}
