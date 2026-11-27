package com.proyecto_final.ms_transaction.application.support;

import com.proyecto_final.ms_transaction.application.port.out.MovementRepositoryPort;
import com.proyecto_final.ms_transaction.application.port.out.client.account.AccountClientPort;
import com.proyecto_final.ms_transaction.application.port.out.client.account.dto.AccountInfoDto;
import com.proyecto_final.ms_transaction.application.validator.AccountMovementRulesValidator;
import com.proyecto_final.ms_transaction.domain.exception.AccountNotFoundException;
import com.proyecto_final.ms_transaction.domain.exception.MonthlyMovementLimitExceededException;
import com.proyecto_final.ms_transaction.domain.exception.MovementDayNotAllowedException;
import com.proyecto_final.ms_transaction.domain.model.ProductCategoryEnum;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.observers.TestObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AccountMovementRulesValidator se simula como colaborador: su propia logica de negocio
 * ya se prueba en AccountMovementRulesValidatorTest. Aqui solo se verifica la orquestacion.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AccountMovementGuard")
class AccountMovementGuardTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-01-10T10:00:00Z");
    private static final Instant MONTH_START = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant NEXT_MONTH_START = Instant.parse("2026-02-01T00:00:00Z");

    @Mock
    private AccountClientPort accountClientPort;

    @Mock
    private MovementRepositoryPort movementRepositoryPort;

    @Mock
    private AccountMovementRulesValidator accountMovementRulesValidator;

    private AccountMovementGuard accountMovementGuard;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
        accountMovementGuard = new AccountMovementGuard(
                accountClientPort, movementRepositoryPort, accountMovementRulesValidator, clock);
    }

    @Nested
    @DisplayName("findOperableAccount")
    class FindOperableAccount {

        @Nested
        @Tag("success-case")
        @DisplayName("Casos de exito")
        class SuccessCases {

            @Test
            @DisplayName("findOperableAccount - cuenta valida dentro de sus limites - retorna la cuenta")
            void findOperableAccount_validAccountWithinLimits_returnsAccount() {
                // Arrange
                AccountInfoDto account = TransactionMother.savingsAccount();
                when(accountClientPort.findAccountById(account.id())).thenReturn(Single.just(account));
                when(movementRepositoryPort.countByProductBetween(
                        ProductCategoryEnum.ACCOUNT, account.id(), MONTH_START, NEXT_MONTH_START))
                        .thenReturn(Single.just(2L));

                // Act
                TestObserver<AccountInfoDto> testObserver =
                        accountMovementGuard.findOperableAccount(account.id()).test();

                // Assert
                testObserver.assertComplete();
                testObserver.assertValue(account);
                verify(accountMovementRulesValidator).validateMovementDay(account, 10);
                verify(accountMovementRulesValidator).validateMonthlyLimit(account, 2L);
            }
        }

        @Nested
        @Tag("error-case")
        @DisplayName("Casos de error")
        class ErrorCases {

            @Test
            @DisplayName("findOperableAccount - la cuenta no existe en ms-account - propaga AccountNotFoundException"
                    + " sin consultar movimientos")
            void findOperableAccount_accountNotFound_propagatesErrorWithoutCountingMovements() {
                // Arrange
                AccountNotFoundException notFound = new AccountNotFoundException("missing-id");
                when(accountClientPort.findAccountById("missing-id")).thenReturn(Single.error(notFound));

                // Act
                TestObserver<AccountInfoDto> testObserver =
                        accountMovementGuard.findOperableAccount("missing-id").test();

                // Assert
                testObserver.assertError(AccountNotFoundException.class);
                verify(movementRepositoryPort, never()).countByProductBetween(any(), any(), any(), any());
            }

            @Test
            @DisplayName("findOperableAccount - el dia no esta permitido - propaga MovementDayNotAllowedException"
                    + " sin consultar movimientos")
            void findOperableAccount_dayNotAllowed_propagatesErrorWithoutCountingMovements() {
                // Arrange
                AccountInfoDto account = TransactionMother.fixedTermAccount(20);
                when(accountClientPort.findAccountById(account.id())).thenReturn(Single.just(account));
                doThrow(new MovementDayNotAllowedException(account.id(), 20))
                        .when(accountMovementRulesValidator).validateMovementDay(account, 10);

                // Act
                TestObserver<AccountInfoDto> testObserver =
                        accountMovementGuard.findOperableAccount(account.id()).test();

                // Assert
                testObserver.assertError(MovementDayNotAllowedException.class);
                verify(movementRepositoryPort, never()).countByProductBetween(any(), any(), any(), any());
            }

            @Test
            @DisplayName("findOperableAccount - se alcanzo el limite mensual - propaga MonthlyMovementLimitExceededException")
            void findOperableAccount_monthlyLimitReached_propagatesError() {
                // Arrange
                AccountInfoDto account = TransactionMother.savingsAccount();
                when(accountClientPort.findAccountById(account.id())).thenReturn(Single.just(account));
                when(movementRepositoryPort.countByProductBetween(
                        ProductCategoryEnum.ACCOUNT, account.id(), MONTH_START, NEXT_MONTH_START))
                        .thenReturn(Single.just(10L));
                doThrow(new MonthlyMovementLimitExceededException(account.id(), account.type()))
                        .when(accountMovementRulesValidator).validateMonthlyLimit(account, 10L);

                // Act
                TestObserver<AccountInfoDto> testObserver =
                        accountMovementGuard.findOperableAccount(account.id()).test();

                // Assert
                testObserver.assertError(MonthlyMovementLimitExceededException.class);
            }
        }

        @Nested
        @Tag("unexpected-case")
        @DisplayName("Casos inesperados")
        class UnexpectedCases {

            @Test
            @DisplayName("findOperableAccount - el conteo de movimientos falla con error no declarado - propaga el error")
            void findOperableAccount_countFailsWithUnexpectedError_propagatesError() {
                // Arrange
                AccountInfoDto account = TransactionMother.savingsAccount();
                RuntimeException databaseError = new RuntimeException("Mongo connection lost");
                when(accountClientPort.findAccountById(account.id())).thenReturn(Single.just(account));
                when(movementRepositoryPort.countByProductBetween(any(), any(), any(), any()))
                        .thenReturn(Single.error(databaseError));

                // Act
                TestObserver<AccountInfoDto> testObserver =
                        accountMovementGuard.findOperableAccount(account.id()).test();

                // Assert
                testObserver.assertError(databaseError);
            }
        }
    }
}
