package com.proyecto_final.ms_transaction.application.service;

import com.proyecto_final.ms_transaction.application.port.in.dto.AccountOperationDto;
import com.proyecto_final.ms_transaction.application.port.in.use_cases.DepositUseCase;
import com.proyecto_final.ms_transaction.application.support.AccountMovementGuard;
import com.proyecto_final.ms_transaction.application.support.BalanceProvider;
import com.proyecto_final.ms_transaction.application.support.MovementRecorder;
import com.proyecto_final.ms_transaction.domain.model.Movement;
import com.proyecto_final.ms_transaction.domain.model.MovementTypeEnum;
import com.proyecto_final.ms_transaction.domain.model.ProductCategoryEnum;
import io.reactivex.rxjava3.core.Single;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;

/**
 * Implementa el caso de uso DepositUseCase: orquesta las reglas del negocio.
 */
@Service
@RequiredArgsConstructor
public class DepositService implements DepositUseCase {

    private final AccountMovementGuard accountMovementGuard;

    private final BalanceProvider balanceProvider;

    private final MovementRecorder movementRecorder;

    private final Clock clock;

    /**
     * {@inheritDoc}
     */
    @Override
    public Single<Movement> deposit(AccountOperationDto accountOperationDto) {
        return accountMovementGuard.findOperableAccount(accountOperationDto.accountId())
                .flatMap(account -> balanceProvider.load(ProductCategoryEnum.ACCOUNT, account.id(), BigDecimal.ZERO))
                .map(balance -> balance.add(accountOperationDto.amount(), clock.instant()))
                .flatMap(updatedBalance -> movementRecorder.record(
                        updatedBalance,
                        MovementTypeEnum.DEPOSIT,
                        accountOperationDto.amount(),
                        accountOperationDto.description())
                );
    }
}
