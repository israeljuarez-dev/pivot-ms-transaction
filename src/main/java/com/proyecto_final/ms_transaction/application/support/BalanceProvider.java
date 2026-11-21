package com.proyecto_final.ms_transaction.application.support;

import com.proyecto_final.ms_transaction.application.port.out.ProductBalanceRepositoryPort;
import com.proyecto_final.ms_transaction.domain.model.ProductBalance;
import com.proyecto_final.ms_transaction.domain.model.ProductCategoryEnum;
import io.reactivex.rxjava3.core.Single;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;

/**
 * Obtiene el saldo actual de un producto; si todavía no tiene movimientos,
 * devuelve su saldo inicial (sin guardarlo).
 */
@Component
@RequiredArgsConstructor
public class BalanceProvider {

    private final ProductBalanceRepositoryPort productBalanceRepositoryPort;

    private final Clock clock;

    /**
     * Carga el saldo guardado del producto, o construye el inicial si no existe.
     *
     * @param productCategory categoría del producto
     * @param productId       identificador del producto
     * @param initialAmount   monto con el que nace el saldo si aún no existe
     * @return el saldo actual del producto
     */
    public Single<ProductBalance> load(
            ProductCategoryEnum productCategory, String productId, BigDecimal initialAmount) {
        // Maybe + switchIfEmpty: si no hay saldo guardado se usa el inicial y el resultado pasa a Single.
        // fromCallable evita calcular el saldo inicial cuando ya existe uno guardado
        return productBalanceRepositoryPort.findByProduct(productCategory, productId)
                .switchIfEmpty(
                        Single.fromCallable(() ->
                                ProductBalance.initial(productCategory, productId, initialAmount, clock.instant())
                        )
                );
    }
}