package com.proyecto_final.ms_transaction.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Movimiento registrado sobre un producto. Es un registro contable inmutable:
 * no se modifica ni se elimina.
 */
@Getter
@Builder(toBuilder = true)
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Movement {

    @EqualsAndHashCode.Include
    private final String id;

    private final ProductCategoryEnum productCategory;

    private final String productId;

    private final MovementTypeEnum type;

    private final BigDecimal amount;

    // Saldo del producto justo después del movimiento
    private final BigDecimal balanceAfter;

    private final String description;

    private final Instant createdAt;
}
