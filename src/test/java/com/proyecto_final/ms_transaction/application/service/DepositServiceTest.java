package com.proyecto_final.ms_transaction.application.service;

import com.proyecto_final.ms_transaction.application.port.in.dto.AccountOperationDto;
import com.proyecto_final.ms_transaction.application.port.out.client.account.dto.AccountInfoDto;
import com.proyecto_final.ms_transaction.application.support.AccountMovementGuard;
import com.proyecto_final.ms_transaction.application.support.BalanceProvider;
import com.proyecto_final.ms_transaction.application.support.MovementRecorder;
import com.proyecto_final.ms_transaction.application.support.TransactionMother;
import com.proyecto_final.ms_transaction.domain.exception.AccountNotFoundException;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * DepositService orquesta AccountMovementGuard, BalanceProvider y MovementRecorder;
 * los tres se simulan como colaboradores, cada uno con su propia suite de pruebas.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DepositService")
class DepositServiceTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-01-10T10:00:00Z");

    @Mock
    private AccountMovementGuard accountMovementGuard;

    @Mock
    private BalanceProvider balanceProvider;

    @Mock
    private MovementRecorder movementRecorder;

    @Captor
    private ArgumentCaptor<ProductBalance> balanceCaptor;

    private DepositService depositService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
        depositService = new DepositService(accountMovementGuard, balanceProvider, movementRecorder, clock);
    }

    @Nested
    @DisplayName("deposit")
    class Deposit {

        @Nested
        @Tag("success-case")
        @DisplayName("Casos de exito")
        class SuccessCases {

            @Test
            @DisplayName("deposit - cuenta operable - suma el monto al saldo y registra el movimiento")
            void deposit_operableAccount_addsAmountToBalanceAndRecordsMovement() {
                // Arrange
                AccountInfoDto account = TransactionMother.savingsAccount();
                AccountOperationDto dto = TransactionMother.accountOperationDto(account.id(), BigDecimal.valueOf(100));
                ProductBalance currentBalance = TransactionMother.accountBalance(BigDecimal.valueOf(500));
                Movement recorded = TransactionMother.movement(
                        ProductCategoryEnum.ACCOUNT, account.id(), MovementTypeEnum.DEPOSIT);
                when(accountMovementGuard.findOperableAccount(account.id())).thenReturn(Single.just(account));
                when(balanceProvider.load(ProductCategoryEnum.ACCOUNT, account.id(), BigDecimal.ZERO))
                        .thenReturn(Single.just(currentBalance));
                when(movementRecorder.record(any(ProductBalance.class), eq(MovementTypeEnum.DEPOSIT),
                        eq(dto.amount()), eq(dto.description())))
                        .thenReturn(Single.just(recorded));

                // Act
                TestObserver<Movement> testObserver = depositService.deposit(dto).test();

                // Assert
                testObserver.assertComplete();
                testObserver.assertValue(recorded);
                verify(movementRecorder).record(balanceCaptor.capture(), eq(MovementTypeEnum.DEPOSIT),
                        eq(dto.amount()), eq(dto.description()));
                assertEquals(BigDecimal.valueOf(600), balanceCaptor.getValue().getAmount());
            }
        }

        @Nested
        @Tag("error-case")
        @DisplayName("Casos de error")
        class ErrorCases {

            @Test
            @DisplayName("deposit - la cuenta no puede operar - propaga el error sin cargar saldo")
            void deposit_accountCannotOperate_propagatesErrorWithoutLoadingBalance() {
                // Arrange
                AccountOperationDto dto = TransactionMother.accountOperationDto("missing-id", BigDecimal.valueOf(100));
                when(accountMovementGuard.findOperableAccount("missing-id"))
                        .thenReturn(Single.error(new AccountNotFoundException("missing-id")));

                // Act
                TestObserver<Movement> testObserver = depositService.deposit(dto).test();

                // Assert
                testObserver.assertError(AccountNotFoundException.class);
                verify(balanceProvider, never()).load(any(), any(), any());
            }
        }

        @Nested
        @Tag("unexpected-case")
        @DisplayName("Casos inesperados")
        class UnexpectedCases {

            @Test
            @DisplayName("deposit - la carga del saldo falla con error no declarado - propaga el error")
            void deposit_balanceLoadFailsWithUnexpectedError_propagatesError() {
                // Arrange
                AccountInfoDto account = TransactionMother.savingsAccount();
                AccountOperationDto dto = TransactionMother.accountOperationDto(account.id(), BigDecimal.valueOf(100));
                RuntimeException databaseError = new RuntimeException("Mongo connection lost");
                when(accountMovementGuard.findOperableAccount(account.id())).thenReturn(Single.just(account));
                when(balanceProvider.load(ProductCategoryEnum.ACCOUNT, account.id(), BigDecimal.ZERO))
                        .thenReturn(Single.error(databaseError));

                // Act
                TestObserver<Movement> testObserver = depositService.deposit(dto).test();

                // Assert
                testObserver.assertError(databaseError);
                verify(movementRecorder, never()).record(any(), any(), any(), any());
            }

            @Test
            @DisplayName("deposit - el registro del movimiento falla con error no declarado - propaga el error")
            void deposit_movementRecordingFailsWithUnexpectedError_propagatesError() {
                // Arrange
                AccountInfoDto account = TransactionMother.savingsAccount();
                AccountOperationDto dto = TransactionMother.accountOperationDto(account.id(), BigDecimal.valueOf(100));
                ProductBalance currentBalance = TransactionMother.accountBalance(BigDecimal.valueOf(500));
                RuntimeException databaseError = new RuntimeException("Mongo write timeout");
                when(accountMovementGuard.findOperableAccount(account.id())).thenReturn(Single.just(account));
                when(balanceProvider.load(ProductCategoryEnum.ACCOUNT, account.id(), BigDecimal.ZERO))
                        .thenReturn(Single.just(currentBalance));
                when(movementRecorder.record(any(), any(), any(), any())).thenReturn(Single.error(databaseError));

                // Act
                TestObserver<Movement> testObserver = depositService.deposit(dto).test();

                // Assert
                testObserver.assertError(databaseError);
            }
        }
    }
}
