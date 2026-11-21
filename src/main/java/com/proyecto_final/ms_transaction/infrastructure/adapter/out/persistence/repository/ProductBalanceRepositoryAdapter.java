package com.proyecto_final.ms_transaction.infrastructure.adapter.out.persistence.repository;

import com.proyecto_final.ms_transaction.application.port.out.ProductBalanceRepositoryPort;
import com.proyecto_final.ms_transaction.domain.exception.ConcurrentBalanceUpdateException;
import com.proyecto_final.ms_transaction.domain.model.ProductBalance;
import com.proyecto_final.ms_transaction.domain.model.ProductCategoryEnum;
import com.proyecto_final.ms_transaction.infrastructure.adapter.out.persistence.mapper.ProductBalancePersistenceMapper;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;

/**
 * Implementa el puerto de salida de saldos sobre Spring Data MongoDB.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductBalanceRepositoryAdapter implements ProductBalanceRepositoryPort {

    private final ProductBalanceRepository productBalanceRepository;

    private final ProductBalancePersistenceMapper productBalancePersistenceMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public Maybe<ProductBalance> findByProduct(ProductCategoryEnum productCategory, String productId) {
        return productBalanceRepository.findById(ProductBalance.buildId(productCategory, productId))
                .map(productBalancePersistenceMapper::toDomain);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Single<ProductBalance> save(ProductBalance productBalance) {
        return productBalanceRepository.save(productBalancePersistenceMapper.toEntity(productBalance))
                .map(productBalancePersistenceMapper::toDomain)
                .onErrorResumeNext(error -> translateConcurrencyError(error, productBalance));
    }

    /**
     * Traduce los errores de concurrencia de Mongo a una excepción del dominio:
     * versión desactualizada (otra operación modificó el saldo) o saldo inicial creado a la vez por dos operaciones.
     */
    private Single<ProductBalance> translateConcurrencyError(Throwable error, ProductBalance productBalance) {
        if (error instanceof OptimisticLockingFailureException || error instanceof DuplicateKeyException) {
            log.warn("Concurrent update detected on the balance of product {}", productBalance.getProductId());
            return Single.error(new ConcurrentBalanceUpdateException(productBalance.getProductId()));
        }

        return Single.error(error);
    }
}
