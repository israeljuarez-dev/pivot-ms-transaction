package com.proyecto_final.ms_transaction.application.service;

import com.proyecto_final.ms_transaction.application.port.in.dto.AccountBalanceDto;
import com.proyecto_final.ms_transaction.application.port.out.client.account.AccountClientPort;
import com.proyecto_final.ms_transaction.application.port.out.client.account.dto.AccountInfoDto;
import com.proyecto_final.ms_transaction.application.support.BalanceProvider;
import com.proyecto_final.ms_transaction.application.support.TransactionMother;
import com.proyecto_final.ms_transaction.domain.exception.AccountNotFoundException;
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

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("FindAccountBalanceService")
class FindAccountBalanceServiceTest {

    @Mock
    private AccountClientPort accountClientPort;

    @Mock
    private BalanceProvider balanceProvider;

    private FindAccountBalanceService findAccountBalanceService;

    @BeforeEach
    void setUp() {
        findAccountBalanceService = new FindAccountBalanceService(accountClientPort, balanceProvider);
    }

    @Nested
    @DisplayName("findBalance")
    class FindBalance {

        @Nested
        @Tag("success-case")
        @DisplayName("Casos de exito")
        class SuccessCases {

            @Test
            @DisplayName("findBalance - cuenta existente - retorna el saldo disponible")
            void findBalance_existingAccount_returnsAvailableBalance() {
                // Arrange
                AccountInfoDto account = TransactionMother.savingsAccount();
                when(accountClientPort.findAccountById(account.id())).thenReturn(Single.just(account));
                when(balanceProvider.load(ProductCategoryEnum.ACCOUNT, account.id(), BigDecimal.ZERO))
                        .thenReturn(Single.just(TransactionMother.accountBalance(BigDecimal.valueOf(750))));

                // Act
                TestObserver<AccountBalanceDto> testObserver =
                        findAccountBalanceService.findBalance(account.id()).test();

                // Assert
                testObserver.assertComplete();
                AccountBalanceDto result = testObserver.values().get(0);
                assertAll(
                        () -> assertEquals(account.id(), result.accountId()),
                        () -> assertEquals(account.type(), result.accountType()),
                        () -> assertEquals(BigDecimal.valueOf(750), result.balance())
                );
            }
        }

        @Nested
        @Tag("error-case")
        @DisplayName("Casos de error")
        class ErrorCases {

            @Test
            @DisplayName("findBalance - la cuenta no existe - propaga AccountNotFoundException")
            void findBalance_accountNotFound_propagatesError() {
                // Arrange
                when(accountClientPort.findAccountById("missing-id"))
                        .thenReturn(Single.error(new AccountNotFoundException("missing-id")));

                // Act
                TestObserver<AccountBalanceDto> testObserver =
                        findAccountBalanceService.findBalance("missing-id").test();

                // Assert
                testObserver.assertError(AccountNotFoundException.class);
            }
        }

        @Nested
        @Tag("unexpected-case")
        @DisplayName("Casos inesperados")
        class UnexpectedCases {

            @Test
            @DisplayName("findBalance - la carga del saldo falla con error no declarado - propaga el error")
            void findBalance_balanceLoadFailsWithUnexpectedError_propagatesError() {
                // Arrange
                AccountInfoDto account = TransactionMother.savingsAccount();
                RuntimeException databaseError = new RuntimeException("Mongo connection lost");
                when(accountClientPort.findAccountById(account.id())).thenReturn(Single.just(account));
                when(balanceProvider.load(ProductCategoryEnum.ACCOUNT, account.id(), BigDecimal.ZERO))
                        .thenReturn(Single.error(databaseError));

                // Act
                TestObserver<AccountBalanceDto> testObserver =
                        findAccountBalanceService.findBalance(account.id()).test();

                // Assert
                testObserver.assertError(databaseError);
            }
        }
    }
}
