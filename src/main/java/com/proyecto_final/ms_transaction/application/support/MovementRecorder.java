package com.proyecto_final.ms_transaction.application.support;

import com.proyecto_final.ms_transaction.application.port.out.MovementRepositoryPort;
import com.proyecto_final.ms_transaction.application.port.out.ProductBalanceRepositoryPort;
import com.proyecto_final.ms_transaction.domain.model.Movement;
import com.proyecto_final.ms_transaction.domain.model.MovementTypeEnum;
import com.proyecto_final.ms_transaction.domain.model.ProductBalance;
import io.reactivex.rxjava3.core.Single;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;

/**
 * Registra una operación: primero guarda el nuevo saldo (con control de concurrencia)
 * y después el movimiento que lo explica.
 */
@Component
@RequiredArgsConstructor
public class MovementRecorder {

    private final ProductBalanceRepositoryPort productBalanceRepositoryPort;

    private final MovementRepositoryPort movementRepositoryPort;

    private final Clock clock;

    /**
     * Guarda el saldo actualizado y registra el movimiento correspondiente.
     *
     * @param updatedBalance saldo ya actualizado con la operación
     * @param type           tipo de movimiento
     * @param amount         monto de la operación
     * @param description    descripción opcional
     * @return el movimiento registrado
     */
    public Single<Movement> record(
            ProductBalance updatedBalance,
            MovementTypeEnum type,
            BigDecimal amount,
            String description
    ) {
        return productBalanceRepositoryPort.save(updatedBalance)
                .flatMap(savedBalance ->
                        movementRepositoryPort.save(buildMovement(savedBalance, type, amount, description))
                );
    }

    /**
     * Construye el movimiento con el saldo resultante de la operación.
     */
    private Movement buildMovement(
            ProductBalance savedBalance,
            MovementTypeEnum type,
            BigDecimal amount,
            String description
    ) {
        return Movement.builder()
                .productCategory(savedBalance.getProductCategory())
                .productId(savedBalance.getProductId())
                .type(type)
                .amount(amount)
                .balanceAfter(savedBalance.getAmount())
                .description(description)
                .createdAt(clock.instant())
                .build();
    }
}
