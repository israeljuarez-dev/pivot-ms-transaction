package com.proyecto_final.ms_transaction.infrastructure.adapter.in.rest.dto.response;

import com.proyecto_final.ms_transaction.domain.model.CreditTypeEnum;
import lombok.Builder;

import java.math.BigDecimal;

/**
 * Respuesta con la deuda pendiente y el crédito disponible de un producto de crédito.
 */
@Builder
public record CreditBalanceResponseDto(
        String creditId,
        CreditTypeEnum creditType,
        BigDecimal outstandingDebt,
        BigDecimal availableCredit
) {
}
