package com.proyecto_final.ms_transaction.application.port.out.client.account;

import com.proyecto_final.ms_transaction.application.port.out.client.account.dto.AccountInfoDto;
import io.reactivex.rxjava3.core.Single;

/**
 * Puerto de salida: consulta de cuentas en ms-account.
 */
public interface AccountClientPort {

    /**
     * Obtiene los datos de una cuenta.
     *
     * @param accountId identificador de la cuenta
     * @return la cuenta, o error AccountNotFoundException si no existe,
     * o ProductServiceUnavailableException si ms-account no está disponible
     */
    Single<AccountInfoDto> findAccountById(String accountId);
}