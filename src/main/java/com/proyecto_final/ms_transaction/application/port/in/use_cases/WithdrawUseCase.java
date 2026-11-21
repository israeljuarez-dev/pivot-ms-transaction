package com.proyecto_final.ms_transaction.application.port.in.use_cases;

import com.proyecto_final.ms_transaction.application.port.in.dto.AccountOperationDto;
import com.proyecto_final.ms_transaction.domain.model.Movement;
import io.reactivex.rxjava3.core.Single;

/**
 * Caso de uso: retirar dinero de una cuenta.
 */
public interface WithdrawUseCase {

    /**
     * Retira dinero validando las reglas del tipo de cuenta y el saldo disponible.
     *
     * @param accountOperationDto cuenta y monto a retirar
     * @return el movimiento registrado
     */
    Single<Movement> withdraw(AccountOperationDto accountOperationDto);
}
