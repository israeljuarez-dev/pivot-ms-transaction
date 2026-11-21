package com.proyecto_final.ms_transaction.infrastructure.adapter.out.persistence.mapper;

import com.proyecto_final.ms_transaction.domain.model.Movement;
import com.proyecto_final.ms_transaction.infrastructure.adapter.out.persistence.entity.MovementEntity;
import org.springframework.stereotype.Component;

/**
 * Mapeo manual entre el movimiento del dominio y su documento de Mongo.
 */
@Component
public class MovementPersistenceMapper {

    /**
     * Convierte el movimiento del dominio en su documento de Mongo.
     */
    public MovementEntity toEntity(Movement movement) {
        return MovementEntity.builder()
                .id(movement.getId())
                .productCategory(movement.getProductCategory())
                .productId(movement.getProductId())
                .type(movement.getType())
                .amount(movement.getAmount())
                .balanceAfter(movement.getBalanceAfter())
                .description(movement.getDescription())
                .createdAt(movement.getCreatedAt())
                .build();
    }

    /**
     * Convierte el documento de Mongo en el movimiento del dominio.
     */
    public Movement toDomain(MovementEntity entity) {
        return Movement.builder()
                .id(entity.getId())
                .productCategory(entity.getProductCategory())
                .productId(entity.getProductId())
                .type(entity.getType())
                .amount(entity.getAmount())
                .balanceAfter(entity.getBalanceAfter())
                .description(entity.getDescription())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
