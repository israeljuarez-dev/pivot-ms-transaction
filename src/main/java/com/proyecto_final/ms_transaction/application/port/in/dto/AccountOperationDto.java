package com.proyecto_final.ms_transaction.application.port.in.dto;

import lombok.Builder;

import java.math.BigDecimal;

/**
 * Datos de una operación sobre una cuenta (depósito o retiro).
 */
@Builder
public record AccountOperationDto(
        String accountId,
        BigDecimal amount,
        String description
) {
}
