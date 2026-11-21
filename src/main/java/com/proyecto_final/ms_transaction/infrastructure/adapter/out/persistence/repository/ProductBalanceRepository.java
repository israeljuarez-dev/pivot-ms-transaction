package com.proyecto_final.ms_transaction.infrastructure.adapter.out.persistence.repository;

import com.proyecto_final.ms_transaction.infrastructure.adapter.out.persistence.entity.ProductBalanceEntity;
import org.springframework.data.repository.reactive.RxJava3CrudRepository;

/**
 * Repositorio Spring Data con tipos de RxJava 3 para los saldos. Se busca por id determinista.
 */
public interface ProductBalanceRepository extends RxJava3CrudRepository<ProductBalanceEntity, String> {
}
