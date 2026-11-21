package com.proyecto_final.ms_transaction.infrastructure.adapter.out.persistence.mapper;

import com.proyecto_final.ms_transaction.domain.model.ProductBalance;
import com.proyecto_final.ms_transaction.infrastructure.adapter.out.persistence.entity.ProductBalanceEntity;
import org.springframework.stereotype.Component;

/**
 * Mapeo manual entre el saldo del dominio y su documento de Mongo.
 */
@Component
public class ProductBalancePersistenceMapper {

    /**
     * Convierte el saldo del dominio en su documento de Mongo.
     */
    public ProductBalanceEntity toEntity(ProductBalance productBalance) {
        return ProductBalanceEntity.builder()
                .id(productBalance.getId())
                .productCategory(productBalance.getProductCategory())
                .productId(productBalance.getProductId())
                .amount(productBalance.getAmount())
                .version(productBalance.getVersion())
                .updatedAt(productBalance.getUpdatedAt())
                .build();
    }

    /**
     * Convierte el documento de Mongo en el saldo del dominio.
     */
    public ProductBalance toDomain(ProductBalanceEntity entity) {
        return ProductBalance.builder()
                .id(entity.getId())
                .productCategory(entity.getProductCategory())
                .productId(entity.getProductId())
                .amount(entity.getAmount())
                .version(entity.getVersion())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
