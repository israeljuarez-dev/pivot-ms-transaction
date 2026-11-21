package com.proyecto_final.ms_transaction.application.port.out.client.credit.dto;

import com.proyecto_final.ms_transaction.domain.model.CreditTypeEnum;

import java.math.BigDecimal;

/**
 * Datos del crédito que ms-transaction necesita conocer (provienen de ms-credit).
 */
public record CreditInfoDto(
        String id,
        String customerId,
        CreditTypeEnum type,
        BigDecimal approvedAmount,
        BigDecimal creditLimit
) {
}
