package com.proyecto_final.ms_transaction.infrastructure.adapter.out.client.credit.dto;

import com.proyecto_final.ms_transaction.domain.model.CreditTypeEnum;

import java.math.BigDecimal;

/**
 * Parte de la respuesta de ms-credit que interesa a ms-transaction. Los demás campos se ignoran.
 */
public record CreditClientResponseDto(
        String id,
        String customerId,
        CreditTypeEnum type,
        BigDecimal approvedAmount,
        BigDecimal creditLimit
) {
}
