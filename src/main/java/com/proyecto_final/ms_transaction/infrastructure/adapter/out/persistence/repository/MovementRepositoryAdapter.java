package com.proyecto_final.ms_transaction.infrastructure.adapter.out.persistence.repository;

import com.proyecto_final.ms_transaction.application.port.out.MovementRepositoryPort;
import com.proyecto_final.ms_transaction.domain.model.Movement;
import com.proyecto_final.ms_transaction.domain.model.ProductCategoryEnum;
import com.proyecto_final.ms_transaction.infrastructure.adapter.out.persistence.mapper.MovementPersistenceMapper;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Implementa el puerto de salida de movimientos sobre Spring Data MongoDB.
 */
@Component
@RequiredArgsConstructor
public class MovementRepositoryAdapter implements MovementRepositoryPort {

    private final MovementRepository movementRepository;

    private final MovementPersistenceMapper movementPersistenceMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public Single<Movement> save(Movement movement) {
        return movementRepository.save(movementPersistenceMapper.toEntity(movement))
                .map(movementPersistenceMapper::toDomain);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Flowable<Movement> findByProduct(ProductCategoryEnum productCategory, String productId) {
        return movementRepository.findByProductCategoryAndProductIdOrderByCreatedAtDesc(productCategory, productId)
                .map(movementPersistenceMapper::toDomain);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Single<Long> countByProductBetween(
            ProductCategoryEnum productCategory,
            String productId,
            Instant from,
            Instant to
    ) {
        return movementRepository.countByProductCategoryAndProductIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                productCategory,
                productId,
                from,
                to
        );
    }
}
