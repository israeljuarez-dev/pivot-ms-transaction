package com.proyecto_final.ms_transaction.domain.exception;

import lombok.Getter;

/**
 * Se lanza cuando el crédito no existe en ms-credit.
 */
@Getter
public class CreditNotFoundException extends RuntimeException {

    private final String creditId;

    public CreditNotFoundException(String creditId) {
        super("Credit not found with id: " + creditId);
        this.creditId = creditId;
    }
}