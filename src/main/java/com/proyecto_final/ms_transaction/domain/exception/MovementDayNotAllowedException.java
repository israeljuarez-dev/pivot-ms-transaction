package com.proyecto_final.ms_transaction.domain.exception;

import lombok.Getter;

/**
 * Se lanza cuando se intenta mover una cuenta a plazo fijo en un día distinto al permitido.
 */
@Getter
public class MovementDayNotAllowedException extends RuntimeException {

    private final String accountId;
    private final Integer allowedDayOfMonth;

    public MovementDayNotAllowedException(String accountId, Integer allowedDayOfMonth) {
        super("Account " + accountId + " only allows movements on day " + allowedDayOfMonth + " of the month");
        this.accountId = accountId;
        this.allowedDayOfMonth = allowedDayOfMonth;
    }
}