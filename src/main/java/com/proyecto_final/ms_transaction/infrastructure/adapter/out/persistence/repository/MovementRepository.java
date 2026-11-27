package com.proyecto_final.ms_transaction.infrastructure.adapter.out.persistence.repository;

import com.proyecto_final.ms_transaction.domain.model.ProductCategoryEnum;
import com.proyecto_final.ms_transaction.infrastructure.adapter.out.persistence.entity.MovementEntity;
import io.reactivex.rxjava3.core.Flowable;
import org.springframework.data.repository.reactive.RxJava3CrudRepository;

/**
 * Repositorio Spring Data con tipos de RxJava 3 para los movimientos.
 * Las consultas se derivan del nombre del método (sin @Query).
 * No se expone un conteo por rango de fechas aquí: Spring Data MongoDB no admite dos
 * restricciones sobre la misma propiedad (createdAt) en un mismo nombre de método derivado
 * (lanza InvalidMongoDbApiUsageException), así que ese conteo se resuelve en el adapter,
 * filtrando en memoria el resultado de este método.
 */
public interface MovementRepository extends RxJava3CrudRepository<MovementEntity, String> {

    Flowable<MovementEntity> findByProductCategoryAndProductIdOrderByCreatedAtDesc(
            ProductCategoryEnum productCategory,
            String productId
    );
}