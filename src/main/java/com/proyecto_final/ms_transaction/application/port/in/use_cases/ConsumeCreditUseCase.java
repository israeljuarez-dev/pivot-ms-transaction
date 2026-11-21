package com.proyecto_final.ms_transaction.application.port.in.use_cases;

import com.proyecto_final.ms_transaction.application.port.in.dto.CreditOperationDto;
import com.proyecto_final.ms_transaction.domain.model.Movement;
import io.reactivex.rxjava3.core.Single;

/**
 * Caso de uso: cargar un consumo a una tarjeta de crédito.
 */
public interface ConsumeCreditUseCase {

    /**
     * Registra un consumo en una tarjeta; no puede superar su límite de crédito.
     *
     * @param creditOperationDto tarjeta y monto del consumo
     * @return el movimiento registrado
     */
    Single<Movement> consume(CreditOperationDto creditOperationDto);
}