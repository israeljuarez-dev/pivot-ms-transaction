package com.proyecto_final.ms_transaction.infrastructure.adapter.out.persistence.entity;

import com.proyecto_final.ms_transaction.domain.model.ProductCategoryEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Documento de MongoDB con el saldo de un producto (colección "product_balances").
 * El id determinista (categoría + producto) impide crear dos saldos para el mismo producto.
 */
@Document(collection = "product_balances")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ProductBalanceEntity {

    @Id
    @EqualsAndHashCode.Include
    private String id;

    private ProductCategoryEnum productCategory;

    private String productId;

    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal amount;

    // Bloqueo optimista: si otra operación modificó el saldo, el guardado falla
    @Version
    private Long version;

    private Instant updatedAt;
}
