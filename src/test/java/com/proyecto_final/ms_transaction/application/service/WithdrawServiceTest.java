package com.proyecto_final.ms_transaction.application.service;

import com.proyecto_final.ms_transaction.application.port.in.dto.AccountOperationDto;
import com.proyecto_final.ms_transaction.application.port.out.client.account.dto.AccountInfoDto;
import com.proyecto_final.ms_transaction.application.support.AccountMovementGuard;
import com.proyecto_final.ms_transaction.application.support.BalanceProvider;
import com.proyecto_final.ms_transaction.application.support.MovementRecorder;
import com.proyecto_final.ms_transaction.application.support.TransactionMother;
import com.proyecto_final.ms_transaction.application.validator.BalanceRulesValidator;
import com.proyecto_final.ms_transaction.domain.exception.AccountNotFoundException;
import com.proyecto_final.ms_transaction.domain.exception.InsufficientFundsException;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("WithdrawService")
class WithdrawServiceTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-01-10T10:00:00Z");

    @Mock
    private AccountMovementGuard accountMovementGuard;

    @Mock
    private BalanceProvider balanceProvider;

    @Mock
    private MovementRecorder movementRecorder;

    @Mock
    private BalanceRulesValidator balanceRulesValidator;

    @Captor
    private ArgumentCaptor<ProductBalance> balanceCaptor;

    private WithdrawService withdrawService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
        withdrawService = new WithdrawService(
                accountMovementGuard, balanceProvider, movementRecorder, balanceRulesValidator, clock);
    }

    @Nested
    @DisplayName("withdraw")
    class Withdraw {

        @Nested
        @Tag("success-case")
        @DisplayName("Casos de exito")
        class SuccessCases {

            @Test
            @DisplayName("withdraw - fondos suficientes - resta el monto del saldo y registra el movimiento")
            void withdraw_sufficientFunds_subtractsAmountFromBalanceAndRecordsMovement() {
                // Arrange
                AccountInfoDto account = TransactionMother.savingsAccount();
                AccountOperationDto dto = TransactionMother.accountOperationDto(account.id(), BigDecimal.valueOf(100));
                ProductBalance currentBalance = TransactionMother.accountBalance(BigDecimal.valueOf(500));
                Movement recorded = TransactionMother.movement(
                        ProductCategoryEnum.ACCOUNT, account.id(), MovementTypeEnum.WITHDRAWAL);
                when(accountMovementGuard.findOperableAccount(account.id())).thenReturn(Single.just(account));
                when(balanceProvider.load(ProductCategoryEnum.ACCOUNT, account.id(), BigDecimal.ZERO))
                        .thenReturn(Single.just(currentBalance));
                when(movementRecorder.record(any(ProductBalance.class), eq(MovementTypeEnum.WITHDRAWAL),
                        eq(dto.amount()), eq(dto.description())))
                        .thenReturn(Single.just(recorded));

                // Act
                TestObserver<Movement> testObserver = withdrawService.withdraw(dto).test();

                // Assert
                testObserver.assertComplete();
                testObserver.assertValue(recorded);
                verify(balanceRulesValidator).validateSufficientFunds(currentBalance, dto.amount(), account.id());
                verify(movementRecorder).record(balanceCaptor.capture(), eq(MovementTypeEnum.WITHDRAWAL),
                        eq(dto.amount()), eq(dto.description()));
                assertEquals(BigDecimal.valueOf(400), balanceCaptor.getValue().getAmount());
            }
        }

        @Nested
        @Tag("error-case")
        @DisplayName("Casos de error")
        class ErrorCases {

            @Test
            @DisplayName("withdraw - la cuenta no puede operar - propaga el error sin cargar saldo")
            void withdraw_accountCannotOperate_propagatesErrorWithoutLoadingBalance() {
                // Arrange
                AccountOperationDto dto = TransactionMother.accountOperationDto("missing-id", BigDecimal.valueOf(100));
                when(accountMovementGuard.findOperableAccount("missing-id"))
                        .thenReturn(Single.error(new AccountNotFoundException("missing-id")));

                // Act
                TestObserver<Movement> testObserver = withdrawService.withdraw(dto).test();

                // Assert
                testObserver.assertError(AccountNotFoundException.class);
                verify(balanceProvider, never()).load(any(), any(), any());
            }

            @Test
            @DisplayName("withdraw - fondos insuficientes - propaga InsufficientFundsException sin registrar movimiento")
            void withdraw_insufficientFunds_propagatesErrorWithoutRecordingMovement() {
                // Arrange
                AccountInfoDto account = TransactionMother.savingsAccount();
                AccountOperationDto dto = TransactionMother.accountOperationDto(account.id(), BigDecimal.valueOf(1000));
                ProductBalance currentBalance = TransactionMother.accountBalance(BigDecimal.valueOf(50));
                when(accountMovementGuard.findOperableAccount(account.id())).thenReturn(Single.just(account));
                when(balanceProvider.load(ProductCategoryEnum.ACCOUNT, account.id(), BigDecimal.ZERO))
                        .thenReturn(Single.just(currentBalance));
                doThrow(new InsufficientFundsException(account.id(), BigDecimal.valueOf(50), BigDecimal.valueOf(1000)))
                        .when(balanceRulesValidator)
                        .validateSufficientFunds(currentBalance, dto.amount(), account.id());

                // Act
                TestObserver<Movement> testObserver = withdrawService.withdraw(dto).test();

                // Assert
                testObserver.assertError(InsufficientFundsException.class);
                verify(movementRecorder, never()).record(any(), any(), any(), any());
            }
        }

        @Nested
        @Tag("unexpected-case")
        @DisplayName("Casos inesperados")
        class UnexpectedCases {

            @Test
            @DisplayName("withdraw - la carga del saldo falla con error no declarado - propaga el error")
            void withdraw_balanceLoadFailsWithUnexpectedError_propagatesError() {
                // Arrange
                AccountInfoDto account = TransactionMother.savingsAccount();
                AccountOperationDto dto = TransactionMother.accountOperationDto(account.id(), BigDecimal.valueOf(100));
                RuntimeException databaseError = new RuntimeException("Mongo connection lost");
                when(accountMovementGuard.findOperableAccount(account.id())).thenReturn(Single.just(account));
                when(balanceProvider.load(ProductCategoryEnum.ACCOUNT, account.id(), BigDecimal.ZERO))
                        .thenReturn(Single.error(databaseError));

                // Act
                TestObserver<Movement> testObserver = withdrawService.withdraw(dto).test();

                // Assert
                testObserver.assertError(databaseError);
            }

            @Test
            @DisplayName("withdraw - el registro del movimiento falla con error no declarado - propaga el error")
            void withdraw_movementRecordingFailsWithUnexpectedError_propagatesError() {
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
                TestObserver<Movement> testObserver = withdrawService.withdraw(dto).test();

                // Assert
                testObserver.assertError(databaseError);
            }
        }
    }
}
