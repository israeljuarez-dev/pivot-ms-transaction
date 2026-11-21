package com.proyecto_final.ms_transaction.domain.exception;

import java.math.BigDecimal;
import lombok.Getter;

/**
 * Se lanza cuando el retiro supera el saldo disponible de la cuenta.
 */
@Getter
public class InsufficientFundsException extends RuntimeException {

    private final String accountId;
    private final BigDecimal availableBalance;
    private final BigDecimal requestedAmount;

    public InsufficientFundsException(String accountId, BigDecimal availableBalance, BigDecimal requestedAmount) {
        super("Insufficient funds in account " + accountId
                + ": available " + availableBalance + ", requested " + requestedAmount);
        this.accountId = accountId;
        this.availableBalance = availableBalance;
        this.requestedAmount = requestedAmount;
    }
}