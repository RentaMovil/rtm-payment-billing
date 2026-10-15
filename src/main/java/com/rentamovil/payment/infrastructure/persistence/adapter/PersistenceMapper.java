package com.rentamovil.payment.infrastructure.persistence.adapter;

import com.rentamovil.payment.domain.entity.BankAccount;
import com.rentamovil.payment.domain.entity.Invoice;
import com.rentamovil.payment.domain.entity.Payment;
import com.rentamovil.payment.domain.enums.PaymentStatus;
import com.rentamovil.payment.infrastructure.persistence.entity.BankAccountEntity;
import com.rentamovil.payment.infrastructure.persistence.entity.InvoiceEntity;
import com.rentamovil.payment.infrastructure.persistence.entity.PaymentEntity;

/** Traduce entre entidades JPA y entidades de dominio. Sin Spring: lo usan los adaptadores. */
public final class PersistenceMapper {

    private PersistenceMapper() {
    }

    public static PaymentEntity toEntity(Payment payment) {
        PaymentEntity entity = new PaymentEntity();
        entity.setId(payment.getId());
        entity.setReservationId(payment.getReservationId());
        entity.setBankAccountId(payment.getBankAccountId());
        entity.setPaymentDate(payment.getPaymentDate());
        entity.setAmount(payment.getAmount());
        entity.setReferenceNumber(payment.getReferenceNumber());
        entity.setReceiptFileUrl(payment.getReceiptFileUrl());
        entity.setStatus(payment.getStatus().name());
        entity.setReviewedBy(payment.getReviewedBy());
        entity.setReviewedAt(payment.getReviewedAt());
        entity.setRejectionReason(payment.getRejectionReason());
        return entity;
    }

    public static Payment toDomain(PaymentEntity entity) {
        return Payment.restore(
                entity.getId(),
                entity.getReservationId(),
                entity.getBankAccountId(),
                entity.getPaymentDate(),
                entity.getAmount(),
                entity.getReferenceNumber(),
                entity.getReceiptFileUrl(),
                PaymentStatus.valueOf(entity.getStatus()),
                entity.getReviewedBy(),
                entity.getReviewedAt(),
                entity.getRejectionReason());
    }

    public static BankAccountEntity toEntity(BankAccount bankAccount) {
        BankAccountEntity entity = new BankAccountEntity();
        entity.setId(bankAccount.getId());
        entity.setBankName(bankAccount.getBankName());
        entity.setAccountHolder(bankAccount.getAccountHolder());
        entity.setQrImageUrl(bankAccount.getQrImageUrl());
        entity.setActive(bankAccount.isActive());
        return entity;
    }

    public static BankAccount toDomain(BankAccountEntity entity) {
        return BankAccount.restore(entity.getId(), entity.getBankName(), entity.getAccountHolder(),
                entity.getQrImageUrl(), entity.isActive());
    }

    public static InvoiceEntity toEntity(Invoice invoice) {
        InvoiceEntity entity = new InvoiceEntity();
        entity.setId(invoice.getId());
        entity.setReservationId(invoice.getReservationId());
        entity.setInvoiceNumber(invoice.getInvoiceNumber());
        entity.setGeneratedDate(invoice.getGeneratedDate());
        entity.setInvoicePdfUrl(invoice.getInvoicePdfUrl());
        return entity;
    }

    public static Invoice toDomain(InvoiceEntity entity) {
        return Invoice.restore(entity.getId(), entity.getReservationId(), entity.getInvoiceNumber(),
                entity.getGeneratedDate(), entity.getInvoicePdfUrl());
    }
}
