package com.proyecto_final.ms_transaction.domain.exception;

import lombok.Getter;

/**
 * Se lanza cuando ms-account o ms-credit no responden o contestan con un error de servidor.
 */
@Getter
public class ProductServiceUnavailableException extends RuntimeException {

    private final String serviceName;
    private final String reason;

    public ProductServiceUnavailableException(String serviceName, String reason) {
        super("Service " + serviceName + " is unavailable: " + reason);
        this.serviceName = serviceName;
        this.reason = reason;
    }
}
