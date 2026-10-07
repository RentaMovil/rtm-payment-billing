package com.rentamovil.payment.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.rentamovil.payment.domain.enums.PaymentStatus;
import com.rentamovil.payment.domain.exception.BusinessRuleException;

/**
 * Tests del dominio: sin Spring, sin base de datos (hexagonal-architecture.md, sección 8).
 * Verifican las invariantes de 02-domain/entities-and-rules.md (Payment.INV-001..INV-003).
 */
class PaymentTest {

    private Payment newPayment() {
        return Payment.create(12, 3, new BigDecimal("400000.00"), "ABC123", "https://res.cloudinary.com/x/receipt.jpg");
    }

    @Test
    @DisplayName("Un pago nuevo nace en PENDING_REVIEW")
    void nacePendingReview() {
        assertThat(newPayment().getStatus()).isEqualTo(PaymentStatus.PENDING_REVIEW);
    }

    @Test
    @DisplayName("El monto debe ser mayor a cero")
    void montoDebeSerPositivo() {
        assertThatThrownBy(() -> Payment.create(12, 3, BigDecimal.ZERO, "ABC123", "https://x/receipt.jpg"))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("Falta el comprobante: no se puede crear el pago")
    void comprobanteObligatorio() {
        assertThatThrownBy(() -> Payment.create(12, 3, new BigDecimal("1000"), "ABC123", " "))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("HU-PAYMENT-002 Escenario 1: aprobar completa reviewedBy/reviewedAt")
    void aprobarCompletaRevision() {
        Payment approved = newPayment().approve(99);

        assertThat(approved.getStatus()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(approved.getReviewedBy()).isEqualTo(99);
        assertThat(approved.getReviewedAt()).isNotNull();
    }

    @Test
    @DisplayName("Un pago ya revisado no se puede volver a aprobar")
    void noSeReAprueba() {
        Payment approved = newPayment().approve(99);

        assertThatThrownBy(() -> approved.approve(99)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("Rechazar sin motivo falla")
    void rechazoSinMotivoFalla() {
        assertThatThrownBy(() -> newPayment().reject(99, " ")).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("HU-PAYMENT-002 Escenario 2-3: rechazar guarda el motivo del Administrador")
    void rechazarGuardaMotivo() {
        Payment rejected = newPayment().reject(99, "El comprobante no coincide");

        assertThat(rejected.getStatus()).isEqualTo(PaymentStatus.REJECTED);
        assertThat(rejected.getRejectionReason()).isEqualTo("El comprobante no coincide");
    }
}
