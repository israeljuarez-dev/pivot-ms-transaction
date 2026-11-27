package com.proyecto_final.ms_transaction.application.service;

import com.proyecto_final.ms_transaction.application.port.in.dto.CreditOperationDto;
import com.proyecto_final.ms_transaction.application.port.out.client.credit.CreditClientPort;
import com.proyecto_final.ms_transaction.application.port.out.client.credit.dto.CreditInfoDto;
import com.proyecto_final.ms_transaction.application.support.BalanceProvider;
import com.proyecto_final.ms_transaction.application.support.MovementRecorder;
import com.proyecto_final.ms_transaction.application.support.TransactionMother;
import com.proyecto_final.ms_transaction.application.validator.BalanceRulesValidator;
import com.proyecto_final.ms_transaction.domain.exception.ConsumptionNotAllowedException;
import com.proyecto_final.ms_transaction.domain.exception.CreditLimitExceededException;
import com.proyecto_final.ms_transaction.domain.exception.CreditNotFoundException;
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
@DisplayName("ConsumeCreditService")
class ConsumeCreditServiceTest {

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

    private ConsumeCreditService consumeCreditService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
        consumeCreditService = new ConsumeCreditService(
                creditClientPort, balanceProvider, movementRecorder, balanceRulesValidator, clock);
    }

    @Nested
    @DisplayName("consume")
    class Consume {

        @Nested
        @Tag("success-case")
        @DisplayName("Casos de exito")
        class SuccessCases {

            @Test
            @DisplayName("consume - tarjeta dentro del limite - suma el consumo al saldo y registra el movimiento")
            void consume_cardWithinLimit_addsConsumptionToBalanceAndRecordsMovement() {
                // Arrange
                CreditInfoDto credit = TransactionMother.creditCard();
                CreditOperationDto dto = TransactionMother.creditOperationDto(credit.id(), BigDecimal.valueOf(100));
                ProductBalance currentBalance = TransactionMother.creditBalance(BigDecimal.valueOf(200));
                Movement recorded = TransactionMother.movement(
                        ProductCategoryEnum.CREDIT, credit.id(), MovementTypeEnum.CONSUMPTION);
                when(creditClientPort.findCreditById(credit.id())).thenReturn(Single.just(credit));
                when(balanceProvider.load(ProductCategoryEnum.CREDIT, credit.id(), BigDecimal.ZERO))
                        .thenReturn(Single.just(currentBalance));
                when(movementRecorder.record(any(ProductBalance.class), eq(MovementTypeEnum.CONSUMPTION),
                        eq(dto.amount()), eq(dto.description())))
                        .thenReturn(Single.just(recorded));

                // Act
                TestObserver<Movement> testObserver = consumeCreditService.consume(dto).test();

                // Assert
                testObserver.assertComplete();
                testObserver.assertValue(recorded);
                verify(balanceRulesValidator).validateConsumptionAllowed(credit);
                verify(balanceRulesValidator).validateWithinCreditLimit(currentBalance, dto.amount(), credit);
                verify(movementRecorder).record(balanceCaptor.capture(), eq(MovementTypeEnum.CONSUMPTION),
                        eq(dto.amount()), eq(dto.description()));
                assertEquals(BigDecimal.valueOf(300), balanceCaptor.getValue().getAmount());
            }
        }

        @Nested
        @Tag("error-case")
        @DisplayName("Casos de error")
        class ErrorCases {

            @Test
            @DisplayName("consume - el credito no existe - propaga CreditNotFoundException sin cargar saldo")
            void consume_creditNotFound_propagatesErrorWithoutLoadingBalance() {
                // Arrange
                CreditOperationDto dto = TransactionMother.creditOperationDto("missing-id", BigDecimal.valueOf(100));
                when(creditClientPort.findCreditById("missing-id"))
                        .thenReturn(Single.error(new CreditNotFoundException("missing-id")));

                // Act
                TestObserver<Movement> testObserver = consumeCreditService.consume(dto).test();

                // Assert
                testObserver.assertError(CreditNotFoundException.class);
                verify(balanceProvider, never()).load(any(), any(), any());
            }

            @Test
            @DisplayName("consume - el credito no es tarjeta - propaga ConsumptionNotAllowedException sin cargar saldo")
            void consume_creditIsNotACard_propagatesErrorWithoutLoadingBalance() {
                // Arrange
                CreditInfoDto loan = TransactionMother.loan();
                CreditOperationDto dto = TransactionMother.creditOperationDto(loan.id(), BigDecimal.valueOf(100));
                when(creditClientPort.findCreditById(loan.id())).thenReturn(Single.just(loan));
                doThrow(new ConsumptionNotAllowedException(loan.id(), loan.type()))
                        .when(balanceRulesValidator).validateConsumptionAllowed(loan);

                // Act
                TestObserver<Movement> testObserver = consumeCreditService.consume(dto).test();

                // Assert
                testObserver.assertError(ConsumptionNotAllowedException.class);
                verify(balanceProvider, never()).load(any(), any(), any());
            }

            @Test
            @DisplayName("consume - el consumo supera el limite - propaga CreditLimitExceededException sin registrar movimiento")
            void consume_consumptionExceedsLimit_propagatesErrorWithoutRecordingMovement() {
                // Arrange
                CreditInfoDto credit = TransactionMother.creditCard();
                CreditOperationDto dto = TransactionMother.creditOperationDto(credit.id(), BigDecimal.valueOf(900));
                ProductBalance currentBalance = TransactionMother.creditBalance(BigDecimal.valueOf(200));
                when(creditClientPort.findCreditById(credit.id())).thenReturn(Single.just(credit));
                when(balanceProvider.load(ProductCategoryEnum.CREDIT, credit.id(), BigDecimal.ZERO))
                        .thenReturn(Single.just(currentBalance));
                doThrow(new CreditLimitExceededException(
                        credit.id(), credit.creditLimit(), BigDecimal.valueOf(200), BigDecimal.valueOf(900)))
                        .when(balanceRulesValidator).validateWithinCreditLimit(currentBalance, dto.amount(), credit);

                // Act
                TestObserver<Movement> testObserver = consumeCreditService.consume(dto).test();

                // Assert
                testObserver.assertError(CreditLimitExceededException.class);
                verify(movementRecorder, never()).record(any(), any(), any(), any());
            }
        }

        @Nested
        @Tag("unexpected-case")
        @DisplayName("Casos inesperados")
        class UnexpectedCases {

            @Test
            @DisplayName("consume - la carga del saldo falla con error no declarado - propaga el error")
            void consume_balanceLoadFailsWithUnexpectedError_propagatesError() {
                // Arrange
                CreditInfoDto credit = TransactionMother.creditCard();
                CreditOperationDto dto = TransactionMother.creditOperationDto(credit.id(), BigDecimal.valueOf(100));
                RuntimeException databaseError = new RuntimeException("Mongo connection lost");
                when(creditClientPort.findCreditById(credit.id())).thenReturn(Single.just(credit));
                when(balanceProvider.load(ProductCategoryEnum.CREDIT, credit.id(), BigDecimal.ZERO))
                        .thenReturn(Single.error(databaseError));

                // Act
                TestObserver<Movement> testObserver = consumeCreditService.consume(dto).test();

                // Assert
                testObserver.assertError(databaseError);
            }

            @Test
            @DisplayName("consume - el registro del movimiento falla con error no declarado - propaga el error")
            void consume_movementRecordingFailsWithUnexpectedError_propagatesError() {
                // Arrange
                CreditInfoDto credit = TransactionMother.creditCard();
                CreditOperationDto dto = TransactionMother.creditOperationDto(credit.id(), BigDecimal.valueOf(100));
                ProductBalance currentBalance = TransactionMother.creditBalance(BigDecimal.valueOf(200));
                RuntimeException databaseError = new RuntimeException("Mongo write timeout");
                when(creditClientPort.findCreditById(credit.id())).thenReturn(Single.just(credit));
                when(balanceProvider.load(ProductCategoryEnum.CREDIT, credit.id(), BigDecimal.ZERO))
                        .thenReturn(Single.just(currentBalance));
                when(movementRecorder.record(any(), any(), any(), any())).thenReturn(Single.error(databaseError));

                // Act
                TestObserver<Movement> testObserver = consumeCreditService.consume(dto).test();

                // Assert
                testObserver.assertError(databaseError);
            }
        }
    }
}
