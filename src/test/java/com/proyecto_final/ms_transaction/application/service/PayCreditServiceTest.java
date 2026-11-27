package com.proyecto_final.ms_transaction.application.service;

import com.proyecto_final.ms_transaction.application.port.in.dto.CreditOperationDto;
import com.proyecto_final.ms_transaction.application.port.out.client.credit.CreditClientPort;
import com.proyecto_final.ms_transaction.application.port.out.client.credit.dto.CreditInfoDto;
import com.proyecto_final.ms_transaction.application.support.BalanceProvider;
import com.proyecto_final.ms_transaction.application.support.MovementRecorder;
import com.proyecto_final.ms_transaction.application.support.TransactionMother;
import com.proyecto_final.ms_transaction.application.validator.BalanceRulesValidator;
import com.proyecto_final.ms_transaction.domain.exception.CreditNotFoundException;
import com.proyecto_final.ms_transaction.domain.exception.PaymentExceedsDebtException;
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
@DisplayName("PayCreditService")
class PayCreditServiceTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-01-10T10:00:00Z");

    @Mock
    private CreditClientPort creditClientPort;

    @Mock
    private BalanceProvider balanceProvider;

    @Mock
    private MovementRecorder movementRecorder;

    @Mock
    private BalanceRulesValidator balanceRulesValidator;

    @Captor
    private ArgumentCaptor<ProductBalance> balanceCaptor;

    private PayCreditService payCreditService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
        payCreditService = new PayCreditService(
                creditClientPort, balanceProvider, movementRecorder, balanceRulesValidator, clock);
    }

    @Nested
    @DisplayName("pay")
    class Pay {

        @Nested
        @Tag("success-case")
        @DisplayName("Casos de exito")
        class SuccessCases {

            @Test
            @DisplayName("pay - pago dentro de la deuda - resta el pago del saldo y registra el movimiento")
            void pay_paymentWithinDebt_subtractsPaymentFromBalanceAndRecordsMovement() {
                // Arrange
                CreditInfoDto credit = TransactionMother.loan();
                CreditOperationDto dto = TransactionMother.creditOperationDto(credit.id(), BigDecimal.valueOf(500));
                ProductBalance currentBalance = TransactionMother.creditBalance(BigDecimal.valueOf(5000));
                Movement recorded = TransactionMother.movement(
                        ProductCategoryEnum.CREDIT, credit.id(), MovementTypeEnum.PAYMENT);
                when(creditClientPort.findCreditById(credit.id())).thenReturn(Single.just(credit));
                when(balanceProvider.load(ProductCategoryEnum.CREDIT, credit.id(), credit.approvedAmount()))
                        .thenReturn(Single.just(currentBalance));
                when(movementRecorder.record(any(ProductBalance.class), eq(MovementTypeEnum.PAYMENT),
                        eq(dto.amount()), eq(dto.description())))
                        .thenReturn(Single.just(recorded));

                // Act
                TestObserver<Movement> testObserver = payCreditService.pay(dto).test();

                // Assert
                testObserver.assertComplete();
                testObserver.assertValue(recorded);
                verify(balanceRulesValidator).validatePaymentWithinDebt(currentBalance, dto.amount(), credit.id());
                verify(movementRecorder).record(balanceCaptor.capture(), eq(MovementTypeEnum.PAYMENT),
                        eq(dto.amount()), eq(dto.description()));
                assertEquals(BigDecimal.valueOf(4500), balanceCaptor.getValue().getAmount());
            }
        }

        @Nested
        @Tag("error-case")
        @DisplayName("Casos de error")
        class ErrorCases {

            @Test
            @DisplayName("pay - el credito no existe - propaga CreditNotFoundException sin cargar saldo")
            void pay_creditNotFound_propagatesErrorWithoutLoadingBalance() {
                // Arrange
                CreditOperationDto dto = TransactionMother.creditOperationDto("missing-id", BigDecimal.valueOf(100));
                when(creditClientPort.findCreditById("missing-id"))
                        .thenReturn(Single.error(new CreditNotFoundException("missing-id")));

                // Act
                TestObserver<Movement> testObserver = payCreditService.pay(dto).test();

                // Assert
                testObserver.assertError(CreditNotFoundException.class);
                verify(balanceProvider, never()).load(any(), any(), any());
            }

            @Test
            @DisplayName("pay - el pago supera la deuda - propaga PaymentExceedsDebtException sin registrar movimiento")
            void pay_paymentExceedsDebt_propagatesErrorWithoutRecordingMovement() {
                // Arrange
                CreditInfoDto credit = TransactionMother.loan();
                CreditOperationDto dto = TransactionMother.creditOperationDto(credit.id(), BigDecimal.valueOf(9000));
                ProductBalance currentBalance = TransactionMother.creditBalance(BigDecimal.valueOf(5000));
                when(creditClientPort.findCreditById(credit.id())).thenReturn(Single.just(credit));
                when(balanceProvider.load(ProductCategoryEnum.CREDIT, credit.id(), credit.approvedAmount()))
                        .thenReturn(Single.just(currentBalance));
                doThrow(new PaymentExceedsDebtException(credit.id(), BigDecimal.valueOf(5000), BigDecimal.valueOf(9000)))
                        .when(balanceRulesValidator).validatePaymentWithinDebt(currentBalance, dto.amount(), credit.id());

                // Act
                TestObserver<Movement> testObserver = payCreditService.pay(dto).test();

                // Assert
                testObserver.assertError(PaymentExceedsDebtException.class);
                verify(movementRecorder, never()).record(any(), any(), any(), any());
            }
        }

        @Nested
        @Tag("unexpected-case")
        @DisplayName("Casos inesperados")
        class UnexpectedCases {

            @Test
            @DisplayName("pay - la carga del saldo falla con error no declarado - propaga el error")
            void pay_balanceLoadFailsWithUnexpectedError_propagatesError() {
                // Arrange
                CreditInfoDto credit = TransactionMother.loan();
                CreditOperationDto dto = TransactionMother.creditOperationDto(credit.id(), BigDecimal.valueOf(500));
                RuntimeException databaseError = new RuntimeException("Mongo connection lost");
                when(creditClientPort.findCreditById(credit.id())).thenReturn(Single.just(credit));
                when(balanceProvider.load(ProductCategoryEnum.CREDIT, credit.id(), credit.approvedAmount()))
                        .thenReturn(Single.error(databaseError));

                // Act
                TestObserver<Movement> testObserver = payCreditService.pay(dto).test();

                // Assert
                testObserver.assertError(databaseError);
            }

            @Test
            @DisplayName("pay - el registro del movimiento falla con error no declarado - propaga el error")
            void pay_movementRecordingFailsWithUnexpectedError_propagatesError() {
                // Arrange
                CreditInfoDto credit = TransactionMother.loan();
                CreditOperationDto dto = TransactionMother.creditOperationDto(credit.id(), BigDecimal.valueOf(500));
                ProductBalance currentBalance = TransactionMother.creditBalance(BigDecimal.valueOf(5000));
                RuntimeException databaseError = new RuntimeException("Mongo write timeout");
                when(creditClientPort.findCreditById(credit.id())).thenReturn(Single.just(credit));
                when(balanceProvider.load(ProductCategoryEnum.CREDIT, credit.id(), credit.approvedAmount()))
                        .thenReturn(Single.just(currentBalance));
                when(movementRecorder.record(any(), any(), any(), any())).thenReturn(Single.error(databaseError));

                // Act
                TestObserver<Movement> testObserver = payCreditService.pay(dto).test();

                // Assert
                testObserver.assertError(databaseError);
            }
        }
    }
}
