package com.proyecto_final.ms_transaction.application.port.in.use_cases;

import com.proyecto_final.ms_transaction.domain.model.Movement;
import io.reactivex.rxjava3.core.Flowable;

/**
 * Caso de uso: consultar los movimientos de una cuenta.
 */
public interface FindAccountMovementsUseCase {

    /**
     * Obtiene los movimientos de una cuenta, del más reciente al más antiguo.
     *
     * @param accountId identificador de la cuenta
     * @return flujo de movimientos, o error AccountNotFoundException si la cuenta no existe
     */
    Flowable<Movement> findMovements(String accountId);
}
