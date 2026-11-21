package com.proyecto_final.ms_transaction.application.port.in.dto;

import lombok.Builder;

import java.math.BigDecimal;

/**
 * Datos de una operación sobre un crédito (pago o consumo).
 */
@Builder
public record CreditOperationDto(
        String creditId,
        BigDecimal amount,
        String description
) {
}
