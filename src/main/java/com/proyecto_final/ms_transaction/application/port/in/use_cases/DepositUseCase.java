package com.proyecto_final.ms_transaction.application.port.in.use_cases;

import com.proyecto_final.ms_transaction.application.port.in.dto.AccountOperationDto;
import com.proyecto_final.ms_transaction.domain.model.Movement;
import io.reactivex.rxjava3.core.Single;

/**
 * Caso de uso: depositar dinero en una cuenta.
 */
public interface DepositUseCase {

    /**
     * Deposita dinero validando las reglas del tipo de cuenta.
     *
     * @param accountOperationDto cuenta y monto a depositar
     * @return el movimiento registrado
     */
    Single<Movement> deposit(AccountOperationDto accountOperationDto);
}
