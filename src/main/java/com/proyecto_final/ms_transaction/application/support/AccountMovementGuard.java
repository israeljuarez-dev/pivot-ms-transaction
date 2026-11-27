package com.proyecto_final.ms_transaction.application.support;

import com.proyecto_final.ms_transaction.application.port.out.MovementRepositoryPort;
import com.proyecto_final.ms_transaction.application.port.out.client.account.AccountClientPort;
import com.proyecto_final.ms_transaction.application.port.out.client.account.dto.AccountInfoDto;
import com.proyecto_final.ms_transaction.application.validator.AccountMovementRulesValidator;
import com.proyecto_final.ms_transaction.domain.model.ProductCategoryEnum;
import io.reactivex.rxjava3.core.Single;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Obtiene una cuenta y verifica que pueda recibir un movimiento hoy:
 * día permitido y límite mensual de movimientos (según su tipo).
 */
@Component
@RequiredArgsConstructor
public class AccountMovementGuard {

    private final AccountClientPort accountClientPort;

    private final MovementRepositoryPort movementRepositoryPort;

    private final AccountMovementRulesValidator accountMovementRulesValidator;

    private final Clock clock;

    /**
     * Consulta la cuenta en ms-account y valida que admita un movimiento hoy.
     *
     * @param accountId identificador de la cuenta
     * @return la cuenta, lista para operar
     */
    public Single<AccountInfoDto> findOperableAccount(String accountId) {
        return accountClientPort.findAccountById(accountId)
                .doOnSuccess(account ->
                        accountMovementRulesValidator.validateMovementDay(account, LocalDate.now(clock).getDayOfMonth())
                )
                .flatMap(this::validateMonthlyLimit);
    }

    /**
     * Cuenta los movimientos del mes calendario actual y valida el límite mensual.
     */
    private Single<AccountInfoDto> validateMonthlyLimit(AccountInfoDto account) {
        YearMonth currentMonth = YearMonth.now(clock);
        Instant monthStart = currentMonth.atDay(1).atStartOfDay(clock.getZone()).toInstant();
        Instant nextMonthStart = currentMonth.plusMonths(1).atDay(1).atStartOfDay(clock.getZone()).toInstant();

        return movementRepositoryPort
                .countByProductBetween(ProductCategoryEnum.ACCOUNT, account.id(), monthStart, nextMonthStart)
                .doOnSuccess(movementsThisMonth ->
                        accountMovementRulesValidator.validateMonthlyLimit(account, movementsThisMonth)
                )
                .map(movementsThisMonth -> account);
    }
}