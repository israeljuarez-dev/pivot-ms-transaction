package com.proyecto_final.ms_transaction.infrastructure.adapter.in.rest.validation;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Constantes de validación de los DTOs de request.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TransactionValidationConstants {

    // Importes de hasta 12 dígitos enteros y 2 decimales
    public static final int AMOUNT_INTEGER_DIGITS = 12;

    public static final int AMOUNT_FRACTION_DIGITS = 2;

    public static final int DESCRIPTION_MAX_LENGTH = 100;
}