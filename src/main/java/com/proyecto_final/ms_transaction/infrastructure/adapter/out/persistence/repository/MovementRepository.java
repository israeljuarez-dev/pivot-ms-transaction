package com.proyecto_final.ms_transaction.infrastructure.adapter.out.persistence.repository;

import com.proyecto_final.ms_transaction.domain.model.ProductCategoryEnum;
import com.proyecto_final.ms_transaction.infrastructure.adapter.out.persistence.entity.MovementEntity;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import org.springframework.data.repository.reactive.RxJava3CrudRepository;

import java.time.Instant;

/**
 * Repositorio Spring Data con tipos de RxJava 3 para los movimientos.
 * Las consultas se derivan del nombre del método (sin @Query).
 */
public interface MovementRepository extends RxJava3CrudRepository<MovementEntity, String> {

    Flowable<MovementEntity> findByProductCategoryAndProductIdOrderByCreatedAtDesc(
            ProductCategoryEnum productCategory,
            String productId
    );

    Single<Long> countByProductCategoryAndProductIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            ProductCategoryEnum productCategory,
            String productId,
            Instant from,
            Instant to
    );
}