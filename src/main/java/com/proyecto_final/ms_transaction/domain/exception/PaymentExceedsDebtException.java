package com.proyecto_final.ms_transaction.domain.exception;

import java.math.BigDecimal;
import lombok.Getter;

/**
 * Se lanza cuando el pago supera la deuda pendiente del crédito.
 */
@Getter
public class PaymentExceedsDebtException extends RuntimeException {

    private final String creditId;
    private final BigDecimal outstandingDebt;
    private final BigDecimal requestedAmount;

    public PaymentExceedsDebtException(String creditId, BigDecimal outstandingDebt, BigDecimal requestedAmount) {
        super("Payment exceeds the outstanding debt of credit " + creditId
                + ": debt " + outstandingDebt + ", payment " + requestedAmount);
        this.creditId = creditId;
        this.outstandingDebt = outstandingDebt;
        this.requestedAmount = requestedAmount;
    }
}
