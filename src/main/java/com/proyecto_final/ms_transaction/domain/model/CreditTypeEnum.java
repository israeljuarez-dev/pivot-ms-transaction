package com.proyecto_final.ms_transaction.domain.model;

import java.math.BigDecimal;

/**
 * Tipos de producto de crédito. Definen si admiten consumos y con cuánta deuda nacen.
 */
public enum CreditTypeEnum {

    LOAN,
    CREDIT_CARD;

    /**
     * Indica si se pueden cargar consumos a este tipo de crédito (solo tarjetas).
     *
     * @return true si admite consumos
     */
    public boolean allowsConsumption() {
        return this == CREDIT_CARD;
    }

    /**
     * Deuda con la que nace el producto: el préstamo debe el monto aprobado,
     * la tarjeta nace sin consumos.
     *
     * @param approvedAmount monto aprobado del crédito
     * @return deuda inicial
     */
    public BigDecimal initialDebt(BigDecimal approvedAmount) {
        return switch (this) {
            case LOAN -> approvedAmount;
            case CREDIT_CARD -> BigDecimal.ZERO;
        };
    }
}
