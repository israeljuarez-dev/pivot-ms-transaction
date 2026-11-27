package com.proyecto_final.ms_transaction.application.support;

import com.proyecto_final.ms_transaction.application.port.out.MovementRepositoryPort;
import com.proyecto_final.ms_transaction.application.port.out.ProductBalanceRepositoryPort;
import com.proyecto_final.ms_transaction.domain.exception.ConcurrentBalanceUpdateException;
import com.proyecto_final.ms_transaction.domain.model.Movement;
import com.proyecto_final.ms_transaction.domain.model.MovementTypeEnum;
import com.proyecto_final.ms_transaction.domain.model.ProductBalance;
import com.proyecto_final.ms_transaction.domain.model.ProductCategoryEnum;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.observers.TestObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MovementRecorder")
class MovementRecorderTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-01-10T10:00:00Z");

    @Mock
    private ProductBalanceRepositoryPort productBalanceRepositoryPort;

    @Mock
    private MovementRepositoryPort movementRepositoryPort;

    @Captor
    private ArgumentCaptor<Movement> movementCaptor;

    private MovementRecorder movementRecorder;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
        movementRecorder = new MovementRecorder(productBalanceRepositoryPort, movementRepositoryPort, clock);
    }

    @Nested
    @DisplayName("record")
    class Record {

        @Nested
        @Tag("success-case")
        @DisplayName("Casos de exito")
        class SuccessCases {

            @Test
            @DisplayName("record - saldo y movimiento validos - guarda el saldo y despues registra el movimiento")
            void record_validBalanceAndMovement_savesBalanceThenRecordsMovement() {
                // Arrange
                ProductBalance updatedBalance = TransactionMother.accountBalance(BigDecimal.valueOf(600));
                when(productBalanceRepositoryPort.save(updatedBalance)).thenReturn(Single.just(updatedBalance));
                when(movementRepositoryPort.save(any(Movement.class)))
                        .thenAnswer(invocation -> Single.just(invocation.getArgument(0)));

                // Act
                TestObserver<Movement> testObserver = movementRecorder.record(
                        updatedBalance, MovementTypeEnum.DEPOSIT, BigDecimal.valueOf(100), "salary").test();

                // Assert
                testObserver.assertComplete();
                verify(movementRepositoryPort).save(movementCaptor.capture());
                Movement recorded = movementCaptor.getValue();
                assertAll(
                        () -> assertEquals(ProductCategoryEnum.ACCOUNT, recorded.getProductCategory()),
                        () -> assertEquals(updatedBalance.getProductId(), recorded.getProductId()),
                        () -> assertEquals(MovementTypeEnum.DEPOSIT, recorded.getType()),
                        () -> assertEquals(BigDecimal.valueOf(100), recorded.getAmount()),
                        () -> assertEquals(updatedBalance.getAmount(), recorded.getBalanceAfter()),
                        () -> assertEquals("salary", recorded.getDescription()),
                        () -> assertEquals(FIXED_INSTANT, recorded.getCreatedAt())
                );
            }
        }

        @Nested
        @Tag("error-case")
        @DisplayName("Casos de error")
        class ErrorCases {

            @Test
            @DisplayName("record - el saldo fue modificado concurrentemente - propaga el error sin registrar el movimiento")
            void record_concurrentBalanceUpdate_propagatesErrorWithoutRecordingMovement() {
                // Arrange
                ProductBalance updatedBalance = TransactionMother.accountBalance(BigDecimal.valueOf(600));
                ConcurrentBalanceUpdateException concurrentError =
                        new ConcurrentBalanceUpdateException(updatedBalance.getProductId());
                when(productBalanceRepositoryPort.save(updatedBalance)).thenReturn(Single.error(concurrentError));

                // Act
                TestObserver<Movement> testObserver = movementRecorder.record(
                        updatedBalance, MovementTypeEnum.DEPOSIT, BigDecimal.valueOf(100), "salary").test();

                // Assert
                testObserver.assertError(ConcurrentBalanceUpdateException.class);
                verify(movementRepositoryPort, never()).save(any());
            }
        }

        @Nested
        @Tag("unexpected-case")
        @DisplayName("Casos inesperados")
        class UnexpectedCases {

            @Test
            @DisplayName("record - el registro del movimiento falla con error no declarado - propaga el error")
            void record_movementSaveFailsWithUnexpectedError_propagatesError() {
                // Arrange
                ProductBalance updatedBalance = TransactionMother.accountBalance(BigDecimal.valueOf(600));
                RuntimeException databaseError = new RuntimeException("Mongo write timeout");
                when(productBalanceRepositoryPort.save(updatedBalance)).thenReturn(Single.just(updatedBalance));
                when(movementRepositoryPort.save(any(Movement.class))).thenReturn(Single.error(databaseError));

                // Act
                TestObserver<Movement> testObserver = movementRecorder.record(
                        updatedBalance, MovementTypeEnum.WITHDRAWAL, BigDecimal.valueOf(50), null).test();

                // Assert
                testObserver.assertError(databaseError);
            }
        }
    }
}
