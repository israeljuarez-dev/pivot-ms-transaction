package com.proyecto_final.ms_transaction.infrastructure.adapter.out.client.account.dto;

import com.proyecto_final.ms_transaction.domain.model.AccountTypeEnum;

/**
 * Parte de la respuesta de ms-account que interesa a ms-transaction. Los demás campos se ignoran.
 */
public record AccountClientResponseDto(
        String id,
        String customerId,
        AccountTypeEnum type,
        Integer monthlyMovementLimit,
        Integer movementDayOfMonth
) {
}
