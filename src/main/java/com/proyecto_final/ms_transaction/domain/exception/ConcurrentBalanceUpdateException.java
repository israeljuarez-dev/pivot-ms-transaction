package com.proyecto_final.ms_transaction.domain.exception;


import lombok.Getter;

/**
 * Se lanza cuando el saldo fue modificado por otra operación simultánea y esta debe reintentarse.
 */
@Getter
public class ConcurrentBalanceUpdateException extends RuntimeException {

    private final String productId;

    public ConcurrentBalanceUpdateException(String productId) {
        super("The balance of product " + productId + " was modified concurrently, retry the operation");
        this.productId = productId;
    }
}
