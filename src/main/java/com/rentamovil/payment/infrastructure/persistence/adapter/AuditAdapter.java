package com.rentamovil.payment.infrastructure.persistence.adapter;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.rentamovil.payment.domain.port.out.AuditPort;
import com.rentamovil.payment.infrastructure.persistence.entity.AuditEntity;
import com.rentamovil.payment.infrastructure.persistence.repository.AuditJpaRepository;

/**
 * Escribe en shared.audit, que crea rtm-iam-db.
 *
 * <p>Si el insert falla, la operación de negocio que ya tuvo éxito no se revierte: se registra
 * el fallo en el log y se sigue. La auditoría no debe tumbar el sistema.
 */
@Component
public class AuditAdapter implements AuditPort {

    private final AuditJpaRepository auditRepository;

    public AuditAdapter(AuditJpaRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    @Override
    public void record(Integer userId, String action, String affectedTable, Integer affectedRecordId,
                       String detailJson) {
        try {
            AuditEntity entity = new AuditEntity();
            entity.setUserId(userId);
            entity.setAction(action);
            entity.setAffectedTable(affectedTable);
            entity.setAffectedRecordId(affectedRecordId);
            entity.setDate(LocalDateTime.now());
            entity.setDetail(detailJson);
            auditRepository.save(entity);
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(AuditAdapter.class)
                    .error("No se pudo escribir la auditoría {} de {}#{}", action, affectedTable,
                            affectedRecordId, e);
        }
    }
}
