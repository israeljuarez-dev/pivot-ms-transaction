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
        // Spring Data MongoDB no permite derivar por nombre dos restricciones sobre
        // la misma propiedad (createdAt >= from AND createdAt < to), así que el conteo
        // se hace en memoria filtrando el historial del producto.
        return movementRepository.findByProductCategoryAndProductIdOrderByCreatedAtDesc(productCategory, productId)
                .filter(movement -> isWithinRange(movement.getCreatedAt(), from, to))
                .count();
    }

    /**
     * Indica si una fecha cae dentro del rango [from, to), igual que $gte/$lt en Mongo.
     */
    private boolean isWithinRange(Instant createdAt, Instant from, Instant to) {
        return !createdAt.isBefore(from) && createdAt.isBefore(to);
    }
}
