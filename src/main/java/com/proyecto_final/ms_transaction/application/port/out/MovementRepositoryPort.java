package com.proyecto_final.ms_transaction.application.port.out;

import com.proyecto_final.ms_transaction.domain.model.Movement;
import com.proyecto_final.ms_transaction.domain.model.ProductCategoryEnum;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

import java.time.Instant;

/**
 * Puerto de salida: persistencia de los movimientos.
 */
public interface MovementRepositoryPort {

    /**
     * Guarda un movimiento.
     *
     * @param movement movimiento a guardar
     * @return el movimiento guardado
     */
    Single<Movement> save(Movement movement);

    /**
     * Obtiene los movimientos de un producto, del más reciente al más antiguo.
     *
     * @param productCategory categoría del producto
     * @param productId       identificador del producto
     * @return flujo de movimientos
     */
    Flowable<Movement> findByProduct(ProductCategoryEnum productCategory, String productId);

    /**
     * Cuenta los movimientos de un producto en un rango de fechas (desde incluido, hasta excluido).
     *
     * @param productCategory categoría del producto
     * @param productId       identificador del producto
     * @param from            inicio del rango (incluido)
     * @param to              fin del rango (excluido)
     * @return cantidad de movimientos en el rango
     */
    Single<Long> countByProductBetween(
            ProductCategoryEnum productCategory,
            String productId,
            Instant from,
            Instant to
    );
}
