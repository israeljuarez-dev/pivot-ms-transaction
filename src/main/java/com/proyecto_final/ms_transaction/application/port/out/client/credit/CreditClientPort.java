package com.proyecto_final.ms_transaction.application.port.out.client.credit;

import com.proyecto_final.ms_transaction.application.port.out.client.credit.dto.CreditInfoDto;
import io.reactivex.rxjava3.core.Single;

/**
 * Puerto de salida: consulta de créditos en ms-credit.
 */
public interface CreditClientPort {

    /**
     * Obtiene los datos de un crédito.
     *
     * @param creditId identificador del crédito
     * @return el crédito, o error CreditNotFoundException si no existe,
     * o ProductServiceUnavailableException si ms-credit no está disponible
     */
    Single<CreditInfoDto> findCreditById(String creditId);
}
