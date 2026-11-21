package com.proyecto_final.ms_transaction.domain.exception;

import com.proyecto_final.ms_transaction.domain.model.CreditTypeEnum;
import lombok.Getter;

/**
 * Se lanza cuando se intenta cargar un consumo a un crédito que no es tarjeta.
 */
@Getter
public class ConsumptionNotAllowedException extends RuntimeException {

    private final String creditId;
    private final CreditTypeEnum creditType;

    public ConsumptionNotAllowedException(String creditId, CreditTypeEnum creditType) {
        super("Consumptions are only allowed on credit cards, credit " + creditId + " is " + creditType);
        this.creditId = creditId;
        this.creditType = creditType;
    }
}
