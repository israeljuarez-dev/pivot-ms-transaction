package com.proyecto_final.ms_transaction.application.validator;

import com.proyecto_final.ms_transaction.application.port.out.client.account.dto.AccountInfoDto;
import com.proyecto_final.ms_transaction.domain.exception.MonthlyMovementLimitExceededException;
import com.proyecto_final.ms_transaction.domain.exception.MovementDayNotAllowedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Aplica las reglas de movimientos propias de cada tipo de cuenta.
 * Las reglas viven en AccountTypeEnum; aquí se registra el rechazo y se lanza la excepción.
 */
@Slf4j
@Component
public class AccountMovementRulesValidator {

    /**
     * Verifica que hoy sea un día permitido para mover la cuenta.
     */
    public void validateMovementDay(AccountInfoDto account, int dayOfMonth) {
        if (!account.type().isMovementDayAllowed(account.movementDayOfMonth(), dayOfMonth)) {
            log.warn("Movement rejected, account {} only allows movements on day {}",
                    account.id(), account.movementDayOfMonth());
            throw new MovementDayNotAllowedException(account.id(), account.movementDayOfMonth());
        }
    }

    /**
     * Verifica que la cuenta no haya alcanzado su máximo de movimientos del mes.
     */
    public void validateMonthlyLimit(AccountInfoDto account, long movementsThisMonth) {
        if (account.type().isMonthlyLimitReached(account.monthlyMovementLimit(), movementsThisMonth)) {
            log.warn("Movement rejected, account {} reached its monthly movement limit", account.id());
            throw new MonthlyMovementLimitExceededException(account.id(), account.type());
        }
    }
}
