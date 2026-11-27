package com.proyecto_final.ms_transaction.infrastructure.adapter.in.rest.controller;

import com.proyecto_final.ms_transaction.application.port.in.dto.CreditBalanceDto;
import com.proyecto_final.ms_transaction.application.port.in.use_cases.ConsumeCreditUseCase;
import com.proyecto_final.ms_transaction.application.port.in.use_cases.FindCreditBalanceUseCase;
import com.proyecto_final.ms_transaction.application.port.in.use_cases.FindCreditMovementsUseCase;
import com.proyecto_final.ms_transaction.application.port.in.use_cases.PayCreditUseCase;
import com.proyecto_final.ms_transaction.application.support.TransactionMother;
import com.proyecto_final.ms_transaction.domain.exception.ConsumptionNotAllowedException;
import com.proyecto_final.ms_transaction.domain.exception.CreditLimitExceededException;
import com.proyecto_final.ms_transaction.domain.exception.CreditNotFoundException;
import com.proyecto_final.ms_transaction.domain.exception.PaymentExceedsDebtException;
import com.proyecto_final.ms_transaction.domain.model.Movement;
import com.proyecto_final.ms_transaction.domain.model.MovementTypeEnum;
import com.proyecto_final.ms_transaction.domain.model.ProductCategoryEnum;
import com.proyecto_final.ms_transaction.infrastructure.adapter.in.rest.dto.request.OperationRequestDto;
import com.proyecto_final.ms_transaction.infrastructure.adapter.in.rest.dto.response.CreditBalanceResponseDto;
import com.proyecto_final.ms_transaction.infrastructure.adapter.in.rest.dto.response.MovementResponseDto;
import com.proyecto_final.ms_transaction.infrastructure.adapter.in.rest.exception.GlobalExceptionHandler;
import com.proyecto_final.ms_transaction.infrastructure.adapter.in.rest.mapper.TransactionRestMapper;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreditTransactionController")
class CreditTransactionControllerTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-01-10T10:00:00Z");

    @Mock
    private PayCreditUseCase payCreditUseCase;

    @Mock
    private ConsumeCreditUseCase consumeCreditUseCase;

    @Mock
    private FindCreditBalanceUseCase findCreditBalanceUseCase;

    @Mock
    private FindCreditMovementsUseCase findCreditMovementsUseCase;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        TransactionRestMapper transactionRestMapper = new TransactionRestMapper();
        CreditTransactionController controller = new CreditTransactionController(
                payCreditUseCase, consumeCreditUseCase, findCreditBalanceUseCase, findCreditMovementsUseCase,
                transactionRestMapper);
        Clock fixedClock = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);

        webTestClient = WebTestClient.bindToController(controller)
                .controllerAdvice(new GlobalExceptionHandler(fixedClock))
                .build();
    }

    @Nested
    @DisplayName("POST /transactions/credits/{creditId}/payments")
    class Pay {

        @Nested
        @Tag("success-case")
        @DisplayName("Casos de exito")
        class SuccessCases {

            @Test
            @DisplayName("pay - solicitud valida - retorna 201 con el movimiento registrado")
            void pay_validRequest_returns201WithRecordedMovement() {
                // Arrange
                Movement movement = TransactionMother.movement(
                        ProductCategoryEnum.CREDIT, TransactionMother.CREDIT_ID, MovementTypeEnum.PAYMENT);
                when(payCreditUseCase.pay(any())).thenReturn(Single.just(movement));

                // Act + Assert
                webTestClient.post().uri("/transactions/credits/{creditId}/payments", TransactionMother.CREDIT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(OperationRequestDto.builder().amount(BigDecimal.valueOf(200)).build())
                        .exchange()
                        .expectStatus().isCreated()
                        .expectBody(MovementResponseDto.class)
                        .value(response -> assertEquals(MovementTypeEnum.PAYMENT, response.type()));
            }
        }

        @Nested
        @Tag("error-case")
        @DisplayName("Casos de error")
        class ErrorCases {

            @Test
            @DisplayName("pay - el credito no existe - retorna 404")
            void pay_creditNotFound_returns404() {
                // Arrange
                when(payCreditUseCase.pay(any())).thenReturn(Single.error(new CreditNotFoundException("missing-id")));

                // Act + Assert
                webTestClient.post().uri("/transactions/credits/{creditId}/payments", "missing-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(OperationRequestDto.builder().amount(BigDecimal.valueOf(200)).build())
                        .exchange()
                        .expectStatus().isNotFound();
            }

            @Test
            @DisplayName("pay - el pago supera la deuda - retorna 422")
            void pay_paymentExceedsDebt_returns422() {
                // Arrange
                when(payCreditUseCase.pay(any())).thenReturn(Single.error(new PaymentExceedsDebtException(
                        TransactionMother.CREDIT_ID, BigDecimal.valueOf(100), BigDecimal.valueOf(900))));

                // Act + Assert
                webTestClient.post().uri("/transactions/credits/{creditId}/payments", TransactionMother.CREDIT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(OperationRequestDto.builder().amount(BigDecimal.valueOf(900)).build())
                        .exchange()
                        .expectStatus().isEqualTo(422);
            }
        }
    }

    @Nested
    @DisplayName("POST /transactions/credits/{creditId}/consumptions")
    class Consume {

        @Nested
        @Tag("success-case")
        @DisplayName("Casos de exito")
        class SuccessCases {

            @Test
            @DisplayName("consume - solicitud valida - retorna 201 con el movimiento registrado")
            void consume_validRequest_returns201WithRecordedMovement() {
                // Arrange
                Movement movement = TransactionMother.movement(
                        ProductCategoryEnum.CREDIT, TransactionMother.CREDIT_ID, MovementTypeEnum.CONSUMPTION);
                when(consumeCreditUseCase.consume(any())).thenReturn(Single.just(movement));

                // Act + Assert
                webTestClient.post()
                        .uri("/transactions/credits/{creditId}/consumptions", TransactionMother.CREDIT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(OperationRequestDto.builder().amount(BigDecimal.valueOf(100)).build())
                        .exchange()
                        .expectStatus().isCreated()
                        .expectBody(MovementResponseDto.class)
                        .value(response -> assertEquals(MovementTypeEnum.CONSUMPTION, response.type()));
            }
        }

        @Nested
        @Tag("error-case")
        @DisplayName("Casos de error")
        class ErrorCases {

            @Test
            @DisplayName("consume - el credito no es tarjeta - retorna 422")
            void consume_creditIsNotACard_returns422() {
                // Arrange
                when(consumeCreditUseCase.consume(any())).thenReturn(Single.error(
                        new ConsumptionNotAllowedException(TransactionMother.CREDIT_ID,
                                TransactionMother.loan().type())));

                // Act + Assert
                webTestClient.post()
                        .uri("/transactions/credits/{creditId}/consumptions", TransactionMother.CREDIT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(OperationRequestDto.builder().amount(BigDecimal.valueOf(100)).build())
                        .exchange()
                        .expectStatus().isEqualTo(422);
            }

            @Test
            @DisplayName("consume - supera el limite de credito - retorna 422")
            void consume_creditLimitExceeded_returns422() {
                // Arrange
                when(consumeCreditUseCase.consume(any())).thenReturn(Single.error(new CreditLimitExceededException(
                        TransactionMother.CREDIT_ID, BigDecimal.valueOf(1000),
                        BigDecimal.valueOf(950), BigDecimal.valueOf(100))));

                // Act + Assert
                webTestClient.post()
                        .uri("/transactions/credits/{creditId}/consumptions", TransactionMother.CREDIT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(OperationRequestDto.builder().amount(BigDecimal.valueOf(100)).build())
                        .exchange()
                        .expectStatus().isEqualTo(422);
            }
        }
    }

    @Nested
    @DisplayName("GET /transactions/credits/{creditId}/balance")
    class FindBalance {

        @Nested
        @Tag("success-case")
        @DisplayName("Casos de exito")
        class SuccessCases {

            @Test
            @DisplayName("findBalance - credito existente - retorna 200 con el saldo")
            void findBalance_existingCredit_returns200WithBalance() {
                // Arrange
                CreditBalanceDto balanceDto = CreditBalanceDto.builder()
                        .creditId(TransactionMother.CREDIT_ID)
                        .creditType(TransactionMother.creditCard().type())
                        .outstandingDebt(BigDecimal.valueOf(300))
                        .availableCredit(BigDecimal.valueOf(700))
                        .build();
                when(findCreditBalanceUseCase.findBalance(TransactionMother.CREDIT_ID))
                        .thenReturn(Single.just(balanceDto));

                // Act + Assert
                webTestClient.get().uri("/transactions/credits/{creditId}/balance", TransactionMother.CREDIT_ID)
                        .exchange()
                        .expectStatus().isOk()
                        .expectBody(CreditBalanceResponseDto.class)
                        .value(response -> assertEquals(BigDecimal.valueOf(700), response.availableCredit()));
            }
        }

        @Nested
        @Tag("error-case")
        @DisplayName("Casos de error")
        class ErrorCases {

            @Test
            @DisplayName("findBalance - el credito no existe - retorna 404")
            void findBalance_missingCredit_returns404() {
                // Arrange
                when(findCreditBalanceUseCase.findBalance("missing-id"))
                        .thenReturn(Single.error(new CreditNotFoundException("missing-id")));

                // Act + Assert
                webTestClient.get().uri("/transactions/credits/{creditId}/balance", "missing-id")
                        .exchange()
                        .expectStatus().isNotFound();
            }
        }
    }

    @Nested
    @DisplayName("GET /transactions/credits/{creditId}/movements")
    class FindMovements {

        @Nested
        @Tag("success-case")
        @DisplayName("Casos de exito")
        class SuccessCases {

            @Test
            @DisplayName("findMovements - credito con movimientos - retorna 200 con la lista")
            void findMovements_creditWithMovements_returns200WithList() {
                // Arrange
                Movement movement = TransactionMother.movement(
                        ProductCategoryEnum.CREDIT, TransactionMother.CREDIT_ID, MovementTypeEnum.CONSUMPTION);
                when(findCreditMovementsUseCase.findMovements(TransactionMother.CREDIT_ID))
                        .thenReturn(Flowable.just(movement));

                // Act + Assert
                webTestClient.get().uri("/transactions/credits/{creditId}/movements", TransactionMother.CREDIT_ID)
                        .exchange()
                        .expectStatus().isOk()
                        .expectBodyList(MovementResponseDto.class)
                        .hasSize(1);
            }
        }

        @Nested
        @Tag("rare-case")
        @DisplayName("Casos poco frecuentes")
        class RareCases {

            @Test
            @DisplayName("findMovements - credito sin movimientos - retorna 200 con lista vacia")
            void findMovements_creditWithoutMovements_returns200WithEmptyList() {
                // Arrange
                when(findCreditMovementsUseCase.findMovements(TransactionMother.CREDIT_ID))
                        .thenReturn(Flowable.empty());

                // Act + Assert
                webTestClient.get().uri("/transactions/credits/{creditId}/movements", TransactionMother.CREDIT_ID)
                        .exchange()
                        .expectStatus().isOk()
                        .expectBodyList(MovementResponseDto.class)
                        .hasSize(0);
            }
        }
    }
}
