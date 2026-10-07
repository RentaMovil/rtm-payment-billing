package com.rentamovil.payment.domain.port.out;

/**
 * Registra una acción en shared.audit (cross-cutting, la crea rtm-iam-db). Cada servicio añade su
 * propio &lt;schema&gt;_app con permiso de INSERT; ver rtm-payment-billing-db/03_dcl.
 */
public interface AuditPort {

    void record(Integer userId, String action, String affectedTable, Integer affectedRecordId,
                String detailJson);
}
