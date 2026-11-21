package com.proyecto_final.ms_transaction.domain.model;

/**
 * Tipos de movimiento: depósito y retiro (cuentas), pago y consumo (créditos).
 */
public enum MovementTypeEnum {
    DEPOSIT,
    WITHDRAWAL,
    PAYMENT,
    CONSUMPTION
}
