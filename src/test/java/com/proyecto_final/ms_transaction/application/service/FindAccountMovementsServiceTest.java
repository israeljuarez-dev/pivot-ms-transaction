package com.proyecto_final.ms_transaction.application.service;

import com.proyecto_final.ms_transaction.application.port.out.MovementRepositoryPort;
import com.proyecto_final.ms_transaction.application.port.out.client.account.AccountClientPort;
import com.proyecto_final.ms_transaction.application.port.out.client.account.dto.AccountInfoDto;
import com.proyecto_final.ms_transaction.application.support.TransactionMother;
import com.proyecto_final.ms_transaction.domain.exception.AccountNotFoundException;
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
@DisplayName("FindAccountMovementsService")
class FindAccountMovementsServiceTest {

    @Mock
    private AccountClientPort accountClientPort;

    @Mock
    private MovementRepositoryPort movementRepositoryPort;

    private FindAccountMovementsService findAccountMovementsService;

    @BeforeEach
    void setUp() {
        findAccountMovementsService = new FindAccountMovementsService(accountClientPort, movementRepositoryPort);
    }

    @Nested
    @DisplayName("findMovements")
    class FindMovements {

        @Nested
        @Tag("success-case")
        @DisplayName("Casos de exito")
        class SuccessCases {

            @Test
            @DisplayName("findMovements - cuenta con movimientos - retorna el flujo de movimientos")
            void findMovements_accountWithMovements_returnsMovementsFlow() {
                // Arrange
                AccountInfoDto account = TransactionMother.savingsAccount();
                Movement movement = TransactionMother.movement(
                        ProductCategoryEnum.ACCOUNT, account.id(), MovementTypeEnum.DEPOSIT);
                when(accountClientPort.findAccountById(account.id())).thenReturn(Single.just(account));
                when(movementRepositoryPort.findByProduct(ProductCategoryEnum.ACCOUNT, account.id()))
                        .thenReturn(Flowable.just(movement));

                // Act
                TestSubscriber<Movement> testSubscriber =
                        findAccountMovementsService.findMovements(account.id()).test();

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
            @DisplayName("findMovements - cuenta sin movimientos - retorna un flujo vacio")
            void findMovements_accountWithoutMovements_returnsEmptyFlow() {
                // Arrange
                AccountInfoDto account = TransactionMother.savingsAccount();
                when(accountClientPort.findAccountById(account.id())).thenReturn(Single.just(account));
                when(movementRepositoryPort.findByProduct(ProductCategoryEnum.ACCOUNT, account.id()))
                        .thenReturn(Flowable.empty());

                // Act
                TestSubscriber<Movement> testSubscriber =
                        findAccountMovementsService.findMovements(account.id()).test();

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
            @DisplayName("findMovements - la cuenta no existe - propaga AccountNotFoundException")
            void findMovements_accountNotFound_propagatesError() {
                // Arrange
                when(accountClientPort.findAccountById("missing-id"))
                        .thenReturn(Single.error(new AccountNotFoundException("missing-id")));

                // Act
                TestSubscriber<Movement> testSubscriber =
                        findAccountMovementsService.findMovements("missing-id").test();

                // Assert
                testSubscriber.assertError(AccountNotFoundException.class);
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
                AccountInfoDto account = TransactionMother.savingsAccount();
                RuntimeException databaseError = new RuntimeException("Mongo connection lost");
                when(accountClientPort.findAccountById(account.id())).thenReturn(Single.just(account));
                when(movementRepositoryPort.findByProduct(ProductCategoryEnum.ACCOUNT, account.id()))
                        .thenReturn(Flowable.error(databaseError));

                // Act
                TestSubscriber<Movement> testSubscriber =
                        findAccountMovementsService.findMovements(account.id()).test();

                // Assert
                testSubscriber.assertError(databaseError);
            }
        }
    }
}