package com.proyecto_final.ms_transaction.application.port.out.client.account.dto;

import com.proyecto_final.ms_transaction.domain.model.AccountTypeEnum;

/**
 * Datos de la cuenta que ms-transaction necesita conocer (provienen de ms-account).
 */
public record AccountInfoDto(
        String id,
        String customerId,
        AccountTypeEnum type,
        Integer monthlyMovementLimit,
        Integer movementDayOfMonth
) {
}
