package com.proyecto_final.ms_transaction.application.support;

import com.proyecto_final.ms_transaction.application.port.out.ProductBalanceRepositoryPort;
import com.proyecto_final.ms_transaction.domain.model.ProductBalance;
import com.proyecto_final.ms_transaction.domain.model.ProductCategoryEnum;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.observers.TestObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("BalanceProvider")
class BalanceProviderTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-01-10T10:00:00Z");

    @Mock
    private ProductBalanceRepositoryPort productBalanceRepositoryPort;

    private BalanceProvider balanceProvider;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
        balanceProvider = new BalanceProvider(productBalanceRepositoryPort, clock);
    }

    @Nested
    @DisplayName("load")
    class Load {

        @Nested
        @Tag("success-case")
        @DisplayName("Casos de exito")
        class SuccessCases {

            @Test
            @DisplayName("load - existe un saldo guardado - retorna el saldo existente")
            void load_existingBalance_returnsExistingBalance() {
                // Arrange
                ProductBalance existing = TransactionMother.accountBalance(BigDecimal.valueOf(300));
                when(productBalanceRepositoryPort.findByProduct(ProductCategoryEnum.ACCOUNT, "acc-001"))
                        .thenReturn(Maybe.just(existing));

                // Act
                TestObserver<ProductBalance> testObserver = balanceProvider
                        .load(ProductCategoryEnum.ACCOUNT, "acc-001", BigDecimal.ZERO).test();

                // Assert
                testObserver.assertComplete();
                testObserver.assertValue(existing);
            }
        }

        @Nested
        @Tag("rare-case")
        @DisplayName("Casos poco frecuentes")
        class RareCases {

            @Test
            @DisplayName("load - no existe saldo guardado - retorna el saldo inicial sin persistirlo")
            void load_noSavedBalance_returnsInitialBalanceWithoutPersistingIt() {
                // Arrange
                when(productBalanceRepositoryPort.findByProduct(ProductCategoryEnum.CREDIT, "cred-001"))
                        .thenReturn(Maybe.empty());

                // Act
                TestObserver<ProductBalance> testObserver = balanceProvider
                        .load(ProductCategoryEnum.CREDIT, "cred-001", BigDecimal.valueOf(1000)).test();

                // Assert
                testObserver.assertComplete();
                ProductBalance initial = testObserver.values().get(0);
                assertAll(
                        () -> assertEquals(ProductCategoryEnum.CREDIT, initial.getProductCategory()),
                        () -> assertEquals("cred-001", initial.getProductId()),
                        () -> assertEquals(BigDecimal.valueOf(1000), initial.getAmount()),
                        () -> assertEquals(FIXED_INSTANT, initial.getUpdatedAt())
                );
            }
        }

        @Nested
        @Tag("unexpected-case")
        @DisplayName("Casos inesperados")
        class UnexpectedCases {

            @Test
            @DisplayName("load - el repositorio falla con error no declarado - propaga el error")
            void load_repositoryFailsWithUnexpectedError_propagatesError() {
                // Arrange
                RuntimeException databaseError = new RuntimeException("Mongo connection lost");
                when(productBalanceRepositoryPort.findByProduct(ProductCategoryEnum.ACCOUNT, "acc-001"))
                        .thenReturn(Maybe.error(databaseError));

                // Act
                TestObserver<ProductBalance> testObserver = balanceProvider
                        .load(ProductCategoryEnum.ACCOUNT, "acc-001", BigDecimal.ZERO).test();

                // Assert
                testObserver.assertError(databaseError);
            }
        }
    }
}
