package com.proyecto_final.ms_transaction.application.port.in.dto;


import com.proyecto_final.ms_transaction.domain.model.CreditTypeEnum;
import lombok.Builder;

import java.math.BigDecimal;

/**
 * Deuda pendiente y crédito disponible de un producto de crédito.
 * El crédito disponible es nulo para los préstamos.
 */
@Builder
public record CreditBalanceDto(
        String creditId,
        CreditTypeEnum creditType,
        BigDecimal outstandingDebt,
        BigDecimal availableCredit
) {
}
