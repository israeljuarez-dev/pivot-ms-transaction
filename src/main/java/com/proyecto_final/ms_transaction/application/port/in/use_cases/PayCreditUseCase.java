package com.proyecto_final.ms_transaction.application.port.in.use_cases;

import com.proyecto_final.ms_transaction.application.port.in.dto.CreditOperationDto;
import com.proyecto_final.ms_transaction.domain.model.Movement;
import io.reactivex.rxjava3.core.Single;

/**
 * Caso de uso: pagar un producto de crédito.
 */
public interface PayCreditUseCase {

    /**
     * Registra un pago que reduce la deuda del crédito; no puede superar la deuda pendiente.
     *
     * @param creditOperationDto crédito y monto a pagar
     * @return el movimiento registrado
     */
    Single<Movement> pay(CreditOperationDto creditOperationDto);
}
