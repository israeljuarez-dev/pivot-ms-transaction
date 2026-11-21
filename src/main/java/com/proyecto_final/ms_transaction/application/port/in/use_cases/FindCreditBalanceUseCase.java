package com.proyecto_final.ms_transaction.application.port.in.use_cases;
import com.proyecto_final.ms_transaction.application.port.in.dto.CreditBalanceDto;
import io.reactivex.rxjava3.core.Single;

/**
 * Caso de uso: consultar la deuda y el crédito disponible de un producto de crédito.
 */
public interface FindCreditBalanceUseCase {

    /**
     * Obtiene la deuda pendiente y el crédito disponible de un producto de crédito.
     *
     * @param creditId identificador del crédito
     * @return el saldo, o error CreditNotFoundException si el crédito no existe
     */
    Single<CreditBalanceDto> findBalance(String creditId);
}
