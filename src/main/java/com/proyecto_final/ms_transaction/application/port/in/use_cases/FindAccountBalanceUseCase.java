package com.proyecto_final.ms_transaction.application.port.in.use_cases;

import com.proyecto_final.ms_transaction.application.port.in.dto.AccountBalanceDto;
import io.reactivex.rxjava3.core.Single;

/**
 * Caso de uso: consultar el saldo disponible de una cuenta.
 */
public interface FindAccountBalanceUseCase {

    /**
     * Obtiene el saldo disponible de una cuenta.
     *
     * @param accountId identificador de la cuenta
     * @return el saldo, o error AccountNotFoundException si la cuenta no existe
     */
    Single<AccountBalanceDto> findBalance(String accountId);
}
