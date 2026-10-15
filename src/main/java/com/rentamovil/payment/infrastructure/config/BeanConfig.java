package com.rentamovil.payment.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.rentamovil.payment.application.usecase.BankAccountUseCaseImpl;
import com.rentamovil.payment.application.usecase.InvoiceUseCaseImpl;
import com.rentamovil.payment.application.usecase.PaymentUseCaseImpl;
import com.rentamovil.payment.domain.port.in.BankAccountUseCases;
import com.rentamovil.payment.domain.port.in.InvoiceUseCases;
import com.rentamovil.payment.domain.port.in.PaymentUseCases;
import com.rentamovil.payment.domain.port.out.AuditPort;
import com.rentamovil.payment.domain.port.out.BankAccountRepositoryPort;
import com.rentamovil.payment.domain.port.out.BookingEventPublisherPort;
import com.rentamovil.payment.domain.port.out.InvoicePdfPort;
import com.rentamovil.payment.domain.port.out.InvoiceRepositoryPort;
import com.rentamovil.payment.domain.port.out.InvoiceStoragePort;
import com.rentamovil.payment.domain.port.out.PaymentRepositoryPort;
import com.rentamovil.payment.domain.port.out.ReservationClientPort;

/**
 * Conecta los casos de uso con sus adaptadores.
 * Los casos de uso no llevan @Service para que el dominio no dependa de Spring.
 */
@Configuration
public class BeanConfig {

    @Bean
    public PaymentUseCases paymentUseCases(PaymentRepositoryPort paymentRepository,
                                           InvoiceRepositoryPort invoiceRepository,
                                           ReservationClientPort reservationClient,
                                           BookingEventPublisherPort events,
                                           InvoicePdfPort invoicePdf,
                                           InvoiceStoragePort invoiceStorage,
                                           AuditPort audit,
                                           @Value("${app.payment-billing.file.allowed-prefix}") String allowedFileUrlPrefix) {
        return new PaymentUseCaseImpl(paymentRepository, invoiceRepository, reservationClient, events,
                invoicePdf, invoiceStorage, audit, allowedFileUrlPrefix);
    }

    @Bean
    public InvoiceUseCases invoiceUseCases(InvoiceRepositoryPort invoiceRepository,
                                           InvoiceStoragePort invoiceStorage,
                                           ReservationClientPort reservationClient) {
        return new InvoiceUseCaseImpl(invoiceRepository, invoiceStorage, reservationClient);
    }

    @Bean
    public BankAccountUseCases bankAccountUseCases(BankAccountRepositoryPort bankAccountRepository,
                                                    AuditPort audit,
                                                    @Value("${app.payment-billing.file.allowed-prefix}") String allowedFileUrlPrefix) {
        return new BankAccountUseCaseImpl(bankAccountRepository, audit, allowedFileUrlPrefix);
    }
}
