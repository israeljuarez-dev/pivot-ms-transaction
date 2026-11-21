package com.proyecto_final.ms_transaction.application.port.out;

import com.proyecto_final.ms_transaction.domain.model.ProductBalance;
import com.proyecto_final.ms_transaction.domain.model.ProductCategoryEnum;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;

/**
 * Puerto de salida: persistencia de los saldos de productos.
 */
public interface ProductBalanceRepositoryPort {

    /**
     * Busca el saldo guardado de un producto.
     *
     * @param productCategory categoría del producto
     * @param productId       identificador del producto
     * @return el saldo, o vacío si el producto aún no tiene movimientos
     */
    Maybe<ProductBalance> findByProduct(ProductCategoryEnum productCategory, String productId);

    /**
     * Guarda el saldo con control de concurrencia.
     *
     * @param productBalance saldo a guardar
     * @return el saldo guardado, o error ConcurrentBalanceUpdateException si otra operación lo modificó
     */
    Single<ProductBalance> save(ProductBalance productBalance);
}
