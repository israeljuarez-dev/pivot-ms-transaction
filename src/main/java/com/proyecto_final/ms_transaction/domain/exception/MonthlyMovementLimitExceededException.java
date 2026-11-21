package com.proyecto_final.ms_transaction.domain.exception;

import com.proyecto_final.ms_transaction.domain.model.AccountTypeEnum;
import lombok.Getter;

/**
 * Se lanza cuando la cuenta ya alcanzó su máximo de movimientos del mes.
 */
@Getter
public class MonthlyMovementLimitExceededException extends RuntimeException {

    private final String accountId;
    private final AccountTypeEnum accountType;

    public MonthlyMovementLimitExceededException(String accountId, AccountTypeEnum accountType) {
        super("Account " + accountId + " of type " + accountType + " reached its monthly movement limit");
        this.accountId = accountId;
        this.accountType = accountType;
    }
}
