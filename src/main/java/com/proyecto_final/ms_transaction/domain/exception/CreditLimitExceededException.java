package com.proyecto_final.ms_transaction.domain.exception;

import java.math.BigDecimal;
import lombok.Getter;

/**
 * Se lanza cuando el consumo hace superar el límite de crédito de la tarjeta.
 */
@Getter
public class CreditLimitExceededException extends RuntimeException {

    private final String creditId;
    private final BigDecimal creditLimit;
    private final BigDecimal consumedAmount;
    private final BigDecimal requestedAmount;

    public CreditLimitExceededException(
            String creditId, BigDecimal creditLimit, BigDecimal consumedAmount, BigDecimal requestedAmount) {
        super("Consumption exceeds the credit limit of credit " + creditId
                + ": limit " + creditLimit + ", consumed " + consumedAmount + ", requested " + requestedAmount);
        this.creditId = creditId;
        this.creditLimit = creditLimit;
        this.consumedAmount = consumedAmount;
        this.requestedAmount = requestedAmount;
    }
}
