package com.proyecto_final.ms_transaction.application.service;

import com.proyecto_final.ms_transaction.application.port.out.MovementRepositoryPort;
import com.proyecto_final.ms_transaction.application.port.out.client.credit.CreditClientPort;
import com.proyecto_final.ms_transaction.application.port.out.client.credit.dto.CreditInfoDto;
import com.proyecto_final.ms_transaction.application.support.TransactionMother;
import com.proyecto_final.ms_transaction.domain.exception.CreditNotFoundException;
import com.proyecto_final.ms_transaction.domain.model.Movement;
import com.proyecto_final.ms_transaction.domain.model.MovementTypeEnum;
import com.proyecto_final.ms_transaction.domain.model.ProductCategoryEnum;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("FindCreditMovementsService")
class FindCreditMovementsServiceTest {

    @Mock
    private CreditClientPort creditClientPort;

    @Mock
    private MovementRepositoryPort movementRepositoryPort;

    private FindCreditMovementsService findCreditMovementsService;

    @BeforeEach
    void setUp() {
        findCreditMovementsService = new FindCreditMovementsService(creditClientPort, movementRepositoryPort);
    }

    @Nested
    @DisplayName("findMovements")
    class FindMovements {

        @Nested
        @Tag("success-case")
        @DisplayName("Casos de exito")
        class SuccessCases {

            @Test
            @DisplayName("findMovements - credito con movimientos - retorna el flujo de movimientos")
            void findMovements_creditWithMovements_returnsMovementsFlow() {
                // Arrange
                CreditInfoDto credit = TransactionMother.creditCard();
                Movement movement = TransactionMother.movement(
                        ProductCategoryEnum.CREDIT, credit.id(), MovementTypeEnum.CONSUMPTION);
                when(creditClientPort.findCreditById(credit.id())).thenReturn(Single.just(credit));
                when(movementRepositoryPort.findByProduct(ProductCategoryEnum.CREDIT, credit.id()))
                        .thenReturn(Flowable.just(movement));

                // Act
                TestSubscriber<Movement> testSubscriber =
                        findCreditMovementsService.findMovements(credit.id()).test();

                // Assert
                testSubscriber.assertComplete();
                testSubscriber.assertValues(movement);
            }
        }

        @Nested
        @Tag("rare-case")
        @DisplayName("Casos poco frecuentes")
        class RareCases {

            @Test
            @DisplayName("findMovements - credito sin movimientos - retorna un flujo vacio")
            void findMovements_creditWithoutMovements_returnsEmptyFlow() {
                // Arrange
                CreditInfoDto credit = TransactionMother.creditCard();
                when(creditClientPort.findCreditById(credit.id())).thenReturn(Single.just(credit));
                when(movementRepositoryPort.findByProduct(ProductCategoryEnum.CREDIT, credit.id()))
                        .thenReturn(Flowable.empty());

                // Act
                TestSubscriber<Movement> testSubscriber =
                        findCreditMovementsService.findMovements(credit.id()).test();

                // Assert
                testSubscriber.assertComplete();
                testSubscriber.assertNoValues();
            }
        }

        @Nested
        @Tag("error-case")
        @DisplayName("Casos de error")
        class ErrorCases {

            @Test
            @DisplayName("findMovements - el credito no existe - propaga CreditNotFoundException")
            void findMovements_creditNotFound_propagatesError() {
                // Arrange
                when(creditClientPort.findCreditById("missing-id"))
                        .thenReturn(Single.error(new CreditNotFoundException("missing-id")));

                // Act
                TestSubscriber<Movement> testSubscriber =
                        findCreditMovementsService.findMovements("missing-id").test();

                // Assert
                testSubscriber.assertError(CreditNotFoundException.class);
            }
        }

        @Nested
        @Tag("unexpected-case")
        @DisplayName("Casos inesperados")
        class UnexpectedCases {

            @Test
            @DisplayName("findMovements - el repositorio falla con error no declarado - propaga el error")
            void findMovements_repositoryFailsWithUnexpectedError_propagatesError() {
                // Arrange
                CreditInfoDto credit = TransactionMother.creditCard();
                RuntimeException databaseError = new RuntimeException("Mongo connection lost");
                when(creditClientPort.findCreditById(credit.id())).thenReturn(Single.just(credit));
                when(movementRepositoryPort.findByProduct(ProductCategoryEnum.CREDIT, credit.id()))
                        .thenReturn(Flowable.error(databaseError));

                // Act
                TestSubscriber<Movement> testSubscriber =
                        findCreditMovementsService.findMovements(credit.id()).test();

                // Assert
                testSubscriber.assertError(databaseError);
            }
        }
    }
}