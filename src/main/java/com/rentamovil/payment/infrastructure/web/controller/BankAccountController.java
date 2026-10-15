package com.rentamovil.payment.infrastructure.web.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.rentamovil.payment.domain.entity.AuthenticatedUser;
import com.rentamovil.payment.domain.port.in.BankAccountUseCases;
import com.rentamovil.payment.infrastructure.web.auth.CurrentUser;
import com.rentamovil.payment.infrastructure.web.dto.BankAccountResponse;
import com.rentamovil.payment.infrastructure.web.dto.CreateBankAccountRequest;
import com.rentamovil.payment.infrastructure.web.dto.SetBankAccountActiveRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** HU-PAYMENT-005: cuentas bancarias de la empresa mostradas en el checkout. */
@RestController
@RequestMapping("/bank-accounts")
@RequiredArgsConstructor
public class BankAccountController {

    private final BankAccountUseCases bankAccounts;

    /** includeInactive=true solo lo puede pedir quien tiene bank-accounts:manage. */
    @GetMapping
    public List<BankAccountResponse> findAll(@CurrentUser AuthenticatedUser currentUser,
                                             @RequestParam(defaultValue = "false") boolean includeInactive) {
        return bankAccounts.findAll(currentUser, includeInactive).stream()
                .map(BankAccountResponse::from).toList();
    }

    /** Nace activa. Exclusivo bank-accounts:manage. */
    @PostMapping
    public ResponseEntity<BankAccountResponse> create(@CurrentUser AuthenticatedUser currentUser,
                                                       @Valid @RequestBody CreateBankAccountRequest request) {
        var created = bankAccounts.create(currentUser, request.getBankName(), request.getAccountHolder(),
                request.getQrImageUrl());
        return ResponseEntity.status(HttpStatus.CREATED).body(BankAccountResponse.from(created));
    }

    /** HU-PAYMENT-005 Escenario 2: sin DELETE, solo activar/desactivar. */
    @PatchMapping("/{id}")
    public BankAccountResponse setActive(@CurrentUser AuthenticatedUser currentUser, @PathVariable Integer id,
                                         @Valid @RequestBody SetBankAccountActiveRequest request) {
        return BankAccountResponse.from(bankAccounts.setActive(currentUser, id, request.getIsActive()));
    }
}
