package com.proyecto_final.ms_transaction.application.service;

import com.proyecto_final.ms_transaction.application.port.in.use_cases.FindAccountMovementsUseCase;
import com.proyecto_final.ms_transaction.application.port.out.MovementRepositoryPort;
import com.proyecto_final.ms_transaction.application.port.out.client.account.AccountClientPort;
import com.proyecto_final.ms_transaction.domain.model.Movement;
import com.proyecto_final.ms_transaction.domain.model.ProductCategoryEnum;
import io.reactivex.rxjava3.core.Flowable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Implementa el caso de uso FindAccountMovementsUseCase: orquesta las reglas del negocio.
 */
@Service
@RequiredArgsConstructor
public class FindAccountMovementsService implements FindAccountMovementsUseCase {

    private final AccountClientPort accountClientPort;

    private final MovementRepositoryPort movementRepositoryPort;

    /**
     * {@inheritDoc}
     */
    @Override
    public Flowable<Movement> findMovements(String accountId) {
        return accountClientPort.findAccountById(accountId)
                .flatMapPublisher(account ->
                        movementRepositoryPort.findByProduct(ProductCategoryEnum.ACCOUNT, account.id()));
    }
}
