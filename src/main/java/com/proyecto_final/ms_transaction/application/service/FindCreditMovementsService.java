package com.proyecto_final.ms_transaction.application.service;

import com.proyecto_final.ms_transaction.application.port.in.use_cases.FindCreditMovementsUseCase;
import com.proyecto_final.ms_transaction.application.port.out.MovementRepositoryPort;
import com.proyecto_final.ms_transaction.application.port.out.client.credit.CreditClientPort;
import com.proyecto_final.ms_transaction.domain.model.Movement;
import com.proyecto_final.ms_transaction.domain.model.ProductCategoryEnum;
import io.reactivex.rxjava3.core.Flowable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Implementa el caso de uso FindCreditMovementsUseCase: orquesta las reglas del negocio.
 */
@Service
@RequiredArgsConstructor
public class FindCreditMovementsService implements FindCreditMovementsUseCase {

    private final CreditClientPort creditClientPort;

    private final MovementRepositoryPort movementRepositoryPort;

    /**
     * {@inheritDoc}
     */
    @Override
    public Flowable<Movement> findMovements(String creditId) {
        return creditClientPort.findCreditById(creditId)
                .flatMapPublisher(credit ->
                        movementRepositoryPort.findByProduct(ProductCategoryEnum.CREDIT, credit.id())
                );
    }
}
