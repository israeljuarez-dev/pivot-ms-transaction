package com.proyecto_final.ms_transaction.infrastructure.adapter.out.persistence.entity;

import com.proyecto_final.ms_transaction.domain.model.MovementTypeEnum;
import com.proyecto_final.ms_transaction.domain.model.ProductCategoryEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Documento de MongoDB que persiste un movimiento (colección "movements").
 */
@Document(collection = "movements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class MovementEntity {

    @Id
    @EqualsAndHashCode.Include
    private String id;

    private ProductCategoryEnum productCategory;

    private String productId;

    private MovementTypeEnum type;

    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal amount;

    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal balanceAfter;

    private String description;

    private Instant createdAt;
}
