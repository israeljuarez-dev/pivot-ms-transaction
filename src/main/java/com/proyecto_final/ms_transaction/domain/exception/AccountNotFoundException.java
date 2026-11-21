package com.proyecto_final.ms_transaction.domain.exception;

import lombok.Getter;

/**
 * Se lanza cuando la cuenta no existe en ms-account.
 */
@Getter
public class AccountNotFoundException extends RuntimeException {

    private final String accountId;

    public AccountNotFoundException(String accountId) {
        super("Account not found with id: " + accountId);
        this.accountId = accountId;
    }
}
