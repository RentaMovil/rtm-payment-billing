package com.rentamovil.payment.infrastructure.web.controller;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.rentamovil.payment.domain.entity.AuthenticatedUser;
import com.rentamovil.payment.domain.enums.PaymentStatus;
import com.rentamovil.payment.domain.port.in.PaymentUseCases;
import com.rentamovil.payment.infrastructure.web.auth.CurrentUser;
import com.rentamovil.payment.infrastructure.web.dto.PaymentResponse;
import com.rentamovil.payment.infrastructure.web.dto.RejectPaymentRequest;
import com.rentamovil.payment.infrastructure.web.dto.UploadPaymentRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** HU-PAYMENT-001 (subir comprobante) y HU-PAYMENT-002 (revisión del Administrador). */
@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentUseCases payments;

    /** HU-PAYMENT-001: nace en PENDING_REVIEW. 409 AMOUNT_MISMATCH si no coincide con la reserva. */
    @PostMapping
    public ResponseEntity<PaymentResponse> upload(@CurrentUser AuthenticatedUser currentUser,
                                                  @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
                                                  @Valid @RequestBody UploadPaymentRequest request) {
        var payment = payments.upload(currentUser, authorization, request.getReservationId(),
                request.getBankAccountId(), request.getAmount(), request.getReferenceNumber(),
                request.getReceiptFileUrl());
        return ResponseEntity.status(HttpStatus.CREATED).body(PaymentResponse.from(payment));
    }

    /** Cola de revisión (filtro opcional por reserva/estado). Exclusivo payments:read. */
    @GetMapping
    public List<PaymentResponse> findAll(@CurrentUser AuthenticatedUser currentUser,
                                         @RequestParam(required = false) Integer reservationId,
                                         @RequestParam(required = false) PaymentStatus status) {
        return payments.findAll(currentUser, reservationId, status).stream().map(PaymentResponse::from).toList();
    }

    /** Detalle de un pago. Exclusivo payments:read. */
    @GetMapping("/{id}")
    public PaymentResponse findById(@CurrentUser AuthenticatedUser currentUser, @PathVariable Integer id) {
        return PaymentResponse.from(payments.findById(currentUser, id));
    }

    /** HU-PAYMENT-002 Escenario 1: genera la Invoice en la misma operación. */
    @PatchMapping("/{id}/approve")
    public PaymentResponse approve(@CurrentUser AuthenticatedUser currentUser, @PathVariable Integer id) {
        return PaymentResponse.from(payments.approve(currentUser, id));
    }

    /** HU-PAYMENT-002 Escenarios 2-3. */
    @PatchMapping("/{id}/reject")
    public PaymentResponse reject(@CurrentUser AuthenticatedUser currentUser, @PathVariable Integer id,
                                  @Valid @RequestBody RejectPaymentRequest request) {
        return PaymentResponse.from(payments.reject(currentUser, id, request.getReason(), request.getOutcome()));
    }
}
