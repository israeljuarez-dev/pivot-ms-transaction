package com.proyecto_final.ms_transaction.infrastructure.adapter.in.rest.dto.response;

import com.proyecto_final.ms_transaction.domain.model.AccountTypeEnum;
import lombok.Builder;

import java.math.BigDecimal;

/**
 * Respuesta con el saldo disponible de una cuenta.
 */
@Builder
public record AccountBalanceResponseDto(
        String accountId,
        AccountTypeEnum accountType,
        BigDecimal balance
) {
}