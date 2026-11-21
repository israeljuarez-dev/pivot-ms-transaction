package com.proyecto_final.ms_transaction.application.service;

import com.proyecto_final.ms_transaction.application.port.in.dto.AccountOperationDto;
import com.proyecto_final.ms_transaction.application.port.in.use_cases.WithdrawUseCase;
import com.proyecto_final.ms_transaction.application.support.AccountMovementGuard;
import com.proyecto_final.ms_transaction.application.support.BalanceProvider;
import com.proyecto_final.ms_transaction.application.support.MovementRecorder;
import com.proyecto_final.ms_transaction.application.validator.BalanceRulesValidator;
import com.proyecto_final.ms_transaction.domain.model.Movement;
import com.proyecto_final.ms_transaction.domain.model.MovementTypeEnum;
import com.proyecto_final.ms_transaction.domain.model.ProductBalance;
import com.proyecto_final.ms_transaction.domain.model.ProductCategoryEnum;
import io.reactivex.rxjava3.core.Single;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;

/**
 * Implementa el caso de uso WithdrawUseCase: orquesta las reglas del negocio.
 */
@Service
@RequiredArgsConstructor
public class WithdrawService implements WithdrawUseCase {

    private final AccountMovementGuard accountMovementGuard;

    private final BalanceProvider balanceProvider;

    private final MovementRecorder movementRecorder;

    private final BalanceRulesValidator balanceRulesValidator;

    private final Clock clock;

    /**
     * {@inheritDoc}
     */
    @Override
    public Single<Movement> withdraw(AccountOperationDto accountOperationDto) {
        return accountMovementGuard.findOperableAccount(accountOperationDto.accountId())
                .flatMap(account -> balanceProvider.load(
                        ProductCategoryEnum.ACCOUNT,
                        account.id(),
                        BigDecimal.ZERO)
                )
                .map(balance -> subtractFunds(balance, accountOperationDto))
                .flatMap(updatedBalance -> movementRecorder.record(
                        updatedBalance,
                        MovementTypeEnum.WITHDRAWAL,
                        accountOperationDto.amount(),
                        accountOperationDto.description())
                );
    }

    /**
     * Verifica que haya fondos y devuelve el saldo con el monto restado.
     */
    private ProductBalance subtractFunds(ProductBalance balance, AccountOperationDto accountOperationDto) {
        balanceRulesValidator.validateSufficientFunds(
                balance,
                accountOperationDto.amount(),
                accountOperationDto.accountId()
        );
        return balance.subtract(accountOperationDto.amount(), clock.instant());
    }
}
