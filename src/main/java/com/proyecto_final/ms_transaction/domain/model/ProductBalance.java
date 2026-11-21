package com.proyecto_final.ms_transaction.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Saldo de un producto. Modelo inmutable: cada cambio produce una nueva instancia.
 * El significado de amount depende de la categoría: dinero disponible (cuenta),
 * deuda pendiente (préstamo) o monto consumido (tarjeta).
 * Su id determinista (categoría + producto) garantiza un solo saldo por producto.
 */
@Getter
@Builder(toBuilder = true)
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ProductBalance {

    @EqualsAndHashCode.Include
    private final String id;

    private final ProductCategoryEnum productCategory;

    private final String productId;

    private final BigDecimal amount;

    private final Long version;

    private final Instant updatedAt;

    /**
     * Crea el saldo inicial de un producto que aún no tiene movimientos.
     *
     * @param productCategory categoría del producto
     * @param productId       identificador del producto
     * @param initialAmount   monto con el que nace el saldo
     * @param now             instante de creación
     * @return saldo inicial sin guardar
     */
    public static ProductBalance initial(
            ProductCategoryEnum productCategory, String productId, BigDecimal initialAmount, Instant now) {
        return ProductBalance.builder()
                .id(buildId(productCategory, productId))
                .productCategory(productCategory)
                .productId(productId)
                .amount(initialAmount)
                .updatedAt(now)
                .build();
    }

    /**
     * Construye el id determinista del saldo de un producto.
     *
     * @param productCategory categoría del producto
     * @param productId       identificador del producto
     * @return id del saldo
     */
    public static String buildId(ProductCategoryEnum productCategory, String productId) {
        return productCategory.name() + ":" + productId;
    }

    /**
     * Indica si el monto actual alcanza para restar el valor indicado.
     *
     * @param value valor a restar
     * @return true si el monto actual es mayor o igual al valor
     */
    public boolean covers(BigDecimal value) {
        return amount.compareTo(value) >= 0;
    }

    /**
     * Indica si, al sumar el valor, el monto actual no supera el límite.
     *
     * @param value valor a sumar
     * @param limit límite máximo permitido
     * @return true si el nuevo monto queda dentro del límite
     */
    public boolean fitsWithin(BigDecimal value, BigDecimal limit) {
        return amount.add(value).compareTo(limit) <= 0;
    }

    /**
     * Devuelve una copia con el valor sumado al monto.
     *
     * @param value valor a sumar
     * @param now   instante de la operación
     * @return copia actualizada
     */
    public ProductBalance add(BigDecimal value, Instant now) {
        return toBuilder()
                .amount(amount.add(value))
                .updatedAt(now)
                .build();
    }

    /**
     * Devuelve una copia con el valor restado del monto.
     *
     * @param value valor a restar
     * @param now   instante de la operación
     * @return copia actualizada
     */
    public ProductBalance subtract(BigDecimal value, Instant now) {
        return toBuilder()
                .amount(amount.subtract(value))
                .updatedAt(now)
                .build();
    }
}