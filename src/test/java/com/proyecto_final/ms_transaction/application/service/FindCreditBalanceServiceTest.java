package com.proyecto_final.ms_transaction.application.service;

import com.proyecto_final.ms_transaction.application.port.in.dto.CreditBalanceDto;
import com.proyecto_final.ms_transaction.application.port.out.client.credit.CreditClientPort;
import com.proyecto_final.ms_transaction.application.port.out.client.credit.dto.CreditInfoDto;
import com.proyecto_final.ms_transaction.application.support.BalanceProvider;
import com.proyecto_final.ms_transaction.application.support.TransactionMother;
import com.proyecto_final.ms_transaction.domain.exception.CreditNotFoundException;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("FindCreditBalanceService")
class FindCreditBalanceServiceTest {

    @Mock
    private CreditClientPort creditClientPort;

    @Mock
    private BalanceProvider balanceProvider;

    private FindCreditBalanceService findCreditBalanceService;

    @BeforeEach
    void setUp() {
        findCreditBalanceService = new FindCreditBalanceService(creditClientPort, balanceProvider);
    }

    @Nested
    @DisplayName("findBalance")
    class FindBalance {

        @Nested
        @Tag("success-case")
        @DisplayName("Casos de exito")
        class SuccessCases {

            @Test
            @DisplayName("findBalance - tarjeta con limite - retorna deuda y credito disponible")
            void findBalance_cardWithLimit_returnsDebtAndAvailableCredit() {
                // Arrange
                CreditInfoDto credit = TransactionMother.creditCard();
                when(creditClientPort.findCreditById(credit.id())).thenReturn(Single.just(credit));
                when(balanceProvider.load(ProductCategoryEnum.CREDIT, credit.id(), BigDecimal.ZERO))
                        .thenReturn(Single.just(TransactionMother.creditBalance(BigDecimal.valueOf(300))));

                // Act
                TestObserver<CreditBalanceDto> testObserver = findCreditBalanceService.findBalance(credit.id()).test();

                // Assert
                testObserver.assertComplete();
                CreditBalanceDto result = testObserver.values().get(0);
                assertAll(
                        () -> assertEquals(credit.id(), result.creditId()),
                        () -> assertEquals(BigDecimal.valueOf(300), result.outstandingDebt()),
                        () -> assertEquals(BigDecimal.valueOf(700), result.availableCredit())
                );
            }
        }

        @Nested
        @Tag("rare-case")
        @DisplayName("Casos poco frecuentes")
        class RareCases {

            @Test
            @DisplayName("findBalance - prestamo sin limite - el credito disponible es nulo")
            void findBalance_loanWithoutLimit_availableCreditIsNull() {
                // Arrange
                CreditInfoDto loan = TransactionMother.loan();
                when(creditClientPort.findCreditById(loan.id())).thenReturn(Single.just(loan));
                when(balanceProvider.load(ProductCategoryEnum.CREDIT, loan.id(), loan.approvedAmount()))
                        .thenReturn(Single.just(TransactionMother.creditBalance(loan.approvedAmount())));

                // Act
                TestObserver<CreditBalanceDto> testObserver = findCreditBalanceService.findBalance(loan.id()).test();

                // Assert
                testObserver.assertComplete();
                assertNull(testObserver.values().get(0).availableCredit());
            }
        }

        @Nested
        @Tag("error-case")
        @DisplayName("Casos de error")
        class ErrorCases {

            @Test
            @DisplayName("findBalance - el credito no existe - propaga CreditNotFoundException")
            void findBalance_creditNotFound_propagatesError() {
                // Arrange
                when(creditClientPort.findCreditById("missing-id"))
                        .thenReturn(Single.error(new CreditNotFoundException("missing-id")));

                // Act
                TestObserver<CreditBalanceDto> testObserver =
                        findCreditBalanceService.findBalance("missing-id").test();

                // Assert
                testObserver.assertError(CreditNotFoundException.class);
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
                CreditInfoDto credit = TransactionMother.creditCard();
                RuntimeException databaseError = new RuntimeException("Mongo connection lost");
                when(creditClientPort.findCreditById(credit.id())).thenReturn(Single.just(credit));
                when(balanceProvider.load(ProductCategoryEnum.CREDIT, credit.id(), BigDecimal.ZERO))
                        .thenReturn(Single.error(databaseError));

                // Act
                TestObserver<CreditBalanceDto> testObserver = findCreditBalanceService.findBalance(credit.id()).test();

                // Assert
                testObserver.assertError(databaseError);
            }
        }
    }
}
