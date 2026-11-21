package com.proyecto_final.ms_transaction.application.port.in.dto;

import com.proyecto_final.ms_transaction.domain.model.AccountTypeEnum;
import lombok.Builder;

import java.math.BigDecimal;

/**
 * Saldo disponible de una cuenta.
 */
@Builder
public record AccountBalanceDto(
        String accountId,
        AccountTypeEnum accountType,
        BigDecimal balance
) {
}
