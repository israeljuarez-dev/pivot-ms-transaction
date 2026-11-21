package com.proyecto_final.ms_transaction.application.port.in.use_cases;

import com.proyecto_final.ms_transaction.domain.model.Movement;
import io.reactivex.rxjava3.core.Flowable;

/**
 * Caso de uso: consultar los movimientos de un producto de crédito.
 */
public interface FindCreditMovementsUseCase {

    /**
     * Obtiene los movimientos de un crédito, del más reciente al más antiguo.
     *
     * @param creditId identificador del crédito
     * @return flujo de movimientos, o error CreditNotFoundException si el crédito no existe
     */
    Flowable<Movement> findMovements(String creditId);
}