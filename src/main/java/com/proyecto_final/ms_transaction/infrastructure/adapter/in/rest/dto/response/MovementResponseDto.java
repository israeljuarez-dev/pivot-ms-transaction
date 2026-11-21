package com.proyecto_final.ms_transaction.infrastructure.adapter.in.rest.dto.response;
import com.proyecto_final.ms_transaction.domain.model.MovementTypeEnum;
import com.proyecto_final.ms_transaction.domain.model.ProductCategoryEnum;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Respuesta con los datos de un movimiento.
 */
@Builder
public record MovementResponseDto(
        String id,
        ProductCategoryEnum productCategory,
        String productId,
        MovementTypeEnum type,
        BigDecimal amount,
        BigDecimal balanceAfter,
        String description,
        Instant createdAt
) {
}