package com.proyecto_final.ms_transaction.infrastructure.adapter.in.rest.dto.request;

import com.proyecto_final.ms_transaction.infrastructure.adapter.in.rest.validation.TransactionValidationConstants;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.math.BigDecimal;

/**
 * Solicitud de una operación (depósito, retiro, pago o consumo): monto y descripción opcional.
 */
@Builder
public record OperationRequestDto(
        @NotNull(message = "amount is required")
        @Positive(message = "amount must be greater than zero")
        @Digits(
                integer = TransactionValidationConstants.AMOUNT_INTEGER_DIGITS,
                fraction = TransactionValidationConstants.AMOUNT_FRACTION_DIGITS,
                message = "amount must have at most 12 integer digits and 2 decimals"
        )
        BigDecimal amount,

        @Size(
                max = TransactionValidationConstants.DESCRIPTION_MAX_LENGTH,
                message = "description must have at most 100 characters"
        )
        String description
) {
}
