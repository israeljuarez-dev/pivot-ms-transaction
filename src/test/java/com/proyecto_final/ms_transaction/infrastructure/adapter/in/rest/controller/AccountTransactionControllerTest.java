package com.proyecto_final.ms_transaction.infrastructure.adapter.in.rest.controller;

import com.proyecto_final.ms_transaction.application.port.in.dto.AccountBalanceDto;
import com.proyecto_final.ms_transaction.application.port.in.use_cases.DepositUseCase;
import com.proyecto_final.ms_transaction.application.port.in.use_cases.FindAccountBalanceUseCase;
import com.proyecto_final.ms_transaction.application.port.in.use_cases.FindAccountMovementsUseCase;
import com.proyecto_final.ms_transaction.application.port.in.use_cases.WithdrawUseCase;
import com.proyecto_final.ms_transaction.application.support.TransactionMother;
import com.proyecto_final.ms_transaction.domain.exception.AccountNotFoundException;
import com.proyecto_final.ms_transaction.domain.exception.ConcurrentBalanceUpdateException;
import com.proyecto_final.ms_transaction.domain.exception.InsufficientFundsException;
import com.proyecto_final.ms_transaction.domain.exception.MovementDayNotAllowedException;
import com.proyecto_final.ms_transaction.domain.exception.ProductServiceUnavailableException;
import com.proyecto_final.ms_transaction.domain.model.Movement;
import com.proyecto_final.ms_transaction.domain.model.MovementTypeEnum;
import com.proyecto_final.ms_transaction.domain.model.ProductCategoryEnum;
import com.proyecto_final.ms_transaction.infrastructure.adapter.in.rest.dto.request.OperationRequestDto;
import com.proyecto_final.ms_transaction.infrastructure.adapter.in.rest.dto.response.AccountBalanceResponseDto;
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

/**
 * Controller test con WebTestClient.bindToController (sin levantar servidor). El
 * GlobalExceptionHandler real se agrega via controllerAdvice para validar los codigos HTTP reales.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AccountTransactionController")
class AccountTransactionControllerTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-01-10T10:00:00Z");

    @Mock
    private DepositUseCase depositUseCase;

    @Mock
    private WithdrawUseCase withdrawUseCase;

    @Mock
    private FindAccountBalanceUseCase findAccountBalanceUseCase;

    @Mock
    private FindAccountMovementsUseCase findAccountMovementsUseCase;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        TransactionRestMapper transactionRestMapper = new TransactionRestMapper();
        AccountTransactionController controller = new AccountTransactionController(
                depositUseCase, withdrawUseCase, findAccountBalanceUseCase, findAccountMovementsUseCase,
                transactionRestMapper);
        Clock fixedClock = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);

        webTestClient = WebTestClient.bindToController(controller)
                .controllerAdvice(new GlobalExceptionHandler(fixedClock))
                .build();
    }

    @Nested
    @DisplayName("POST /transactions/accounts/{accountId}/deposits")
    class Deposit {

        @Nested
        @Tag("success-case")
        @DisplayName("Casos de exito")
        class SuccessCases {

            @Test
            @DisplayName("deposit - solicitud valida - retorna 201 con el movimiento registrado")
            void deposit_validRequest_returns201WithRecordedMovement() {
                // Arrange
                Movement movement = TransactionMother.movement(
                        ProductCategoryEnum.ACCOUNT, TransactionMother.ACCOUNT_ID, MovementTypeEnum.DEPOSIT);
                when(depositUseCase.deposit(any())).thenReturn(Single.just(movement));

                // Act + Assert
                webTestClient.post().uri("/transactions/accounts/{accountId}/deposits", TransactionMother.ACCOUNT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(OperationRequestDto.builder().amount(BigDecimal.valueOf(100)).build())
                        .exchange()
                        .expectStatus().isCreated()
                        .expectBody(MovementResponseDto.class)
                        .value(response -> {
                            assert response != null;
                            assertEquals(MovementTypeEnum.DEPOSIT, response.type());
                        });
            }
        }

        @Nested
        @Tag("error-case")
        @DisplayName("Casos de error")
        class ErrorCases {

            @Test
            @DisplayName("deposit - la cuenta no existe - retorna 404")
            void deposit_accountNotFound_returns404() {
                // Arrange
                when(depositUseCase.deposit(any()))
                        .thenReturn(Single.error(new AccountNotFoundException("missing-id")));

                // Act + Assert
                webTestClient.post().uri("/transactions/accounts/{accountId}/deposits", "missing-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(OperationRequestDto.builder().amount(BigDecimal.valueOf(100)).build())
                        .exchange()
                        .expectStatus().isNotFound();
            }

            @Test
            @DisplayName("deposit - dia no permitido para plazo fijo - retorna 422")
            void deposit_dayNotAllowed_returns422() {
                // Arrange
                when(depositUseCase.deposit(any()))
                        .thenReturn(Single.error(new MovementDayNotAllowedException(TransactionMother.ACCOUNT_ID, 15)));

                // Act + Assert
                webTestClient.post().uri("/transactions/accounts/{accountId}/deposits", TransactionMother.ACCOUNT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(OperationRequestDto.builder().amount(BigDecimal.valueOf(100)).build())
                        .exchange()
                        .expectStatus().isEqualTo(422);
            }

            @Test
            @DisplayName("deposit - ms-account no disponible - retorna 503")
            void deposit_accountServiceUnavailable_returns503() {
                // Arrange
                when(depositUseCase.deposit(any()))
                        .thenReturn(Single.error(new ProductServiceUnavailableException("ms-account", "timeout")));

                // Act + Assert
                webTestClient.post().uri("/transactions/accounts/{accountId}/deposits", TransactionMother.ACCOUNT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(OperationRequestDto.builder().amount(BigDecimal.valueOf(100)).build())
                        .exchange()
                        .expectStatus().isEqualTo(503);
            }
        }
    }

    @Nested
    @DisplayName("POST /transactions/accounts/{accountId}/withdrawals")
    class Withdraw {

        @Nested
        @Tag("success-case")
        @DisplayName("Casos de exito")
        class SuccessCases {

            @Test
            @DisplayName("withdraw - solicitud valida - retorna 201 con el movimiento registrado")
            void withdraw_validRequest_returns201WithRecordedMovement() {
                // Arrange
                Movement movement = TransactionMother.movement(
                        ProductCategoryEnum.ACCOUNT, TransactionMother.ACCOUNT_ID, MovementTypeEnum.WITHDRAWAL);
                when(withdrawUseCase.withdraw(any())).thenReturn(Single.just(movement));

                // Act + Assert
                webTestClient.post()
                        .uri("/transactions/accounts/{accountId}/withdrawals", TransactionMother.ACCOUNT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(OperationRequestDto.builder().amount(BigDecimal.valueOf(50)).build())
                        .exchange()
                        .expectStatus().isCreated()
                        .expectBody(MovementResponseDto.class)
                        .value(response -> {
                            assert response != null;
                            assertEquals(MovementTypeEnum.WITHDRAWAL, response.type());
                        });
            }
        }

        @Nested
        @Tag("error-case")
        @DisplayName("Casos de error")
        class ErrorCases {

            @Test
            @DisplayName("withdraw - fondos insuficientes - retorna 422")
            void withdraw_insufficientFunds_returns422() {
                // Arrange
                when(withdrawUseCase.withdraw(any())).thenReturn(Single.error(new InsufficientFundsException(
                        TransactionMother.ACCOUNT_ID, BigDecimal.TEN, BigDecimal.valueOf(500))));

                // Act + Assert
                webTestClient.post()
                        .uri("/transactions/accounts/{accountId}/withdrawals", TransactionMother.ACCOUNT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(OperationRequestDto.builder().amount(BigDecimal.valueOf(500)).build())
                        .exchange()
                        .expectStatus().isEqualTo(422);
            }

            @Test
            @DisplayName("withdraw - saldo modificado concurrentemente - retorna 409")
            void withdraw_concurrentBalanceUpdate_returns409() {
                // Arrange
                when(withdrawUseCase.withdraw(any())).thenReturn(Single.error(
                        new ConcurrentBalanceUpdateException(TransactionMother.ACCOUNT_ID)));

                // Act + Assert
                webTestClient.post()
                        .uri("/transactions/accounts/{accountId}/withdrawals", TransactionMother.ACCOUNT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(OperationRequestDto.builder().amount(BigDecimal.valueOf(50)).build())
                        .exchange()
                        .expectStatus().isEqualTo(409);
            }
        }
    }

    @Nested
    @DisplayName("GET /transactions/accounts/{accountId}/balance")
    class FindBalance {

        @Nested
        @Tag("success-case")
        @DisplayName("Casos de exito")
        class SuccessCases {

            @Test
            @DisplayName("findBalance - cuenta existente - retorna 200 con el saldo")
            void findBalance_existingAccount_returns200WithBalance() {
                // Arrange
                AccountBalanceDto balanceDto = AccountBalanceDto.builder()
                        .accountId(TransactionMother.ACCOUNT_ID)
                        .accountType(TransactionMother.savingsAccount().type())
                        .balance(BigDecimal.valueOf(900))
                        .build();
                when(findAccountBalanceUseCase.findBalance(TransactionMother.ACCOUNT_ID))
                        .thenReturn(Single.just(balanceDto));

                // Act + Assert
                webTestClient.get().uri("/transactions/accounts/{accountId}/balance", TransactionMother.ACCOUNT_ID)
                        .exchange()
                        .expectStatus().isOk()
                        .expectBody(AccountBalanceResponseDto.class)
                        .value(response -> {
                            assert response != null;
                            assertEquals(BigDecimal.valueOf(900), response.balance());
                        });
            }
        }

        @Nested
        @Tag("error-case")
        @DisplayName("Casos de error")
        class ErrorCases {

            @Test
            @DisplayName("findBalance - la cuenta no existe - retorna 404")
            void findBalance_missingAccount_returns404() {
                // Arrange
                when(findAccountBalanceUseCase.findBalance("missing-id"))
                        .thenReturn(Single.error(new AccountNotFoundException("missing-id")));

                // Act + Assert
                webTestClient.get().uri("/transactions/accounts/{accountId}/balance", "missing-id")
                        .exchange()
                        .expectStatus().isNotFound();
            }
        }
    }

    @Nested
    @DisplayName("GET /transactions/accounts/{accountId}/movements")
    class FindMovements {

        @Nested
        @Tag("success-case")
        @DisplayName("Casos de exito")
        class SuccessCases {

            @Test
            @DisplayName("findMovements - cuenta con movimientos - retorna 200 con la lista")
            void findMovements_accountWithMovements_returns200WithList() {
                // Arrange
                Movement movement = TransactionMother.movement(
                        ProductCategoryEnum.ACCOUNT, TransactionMother.ACCOUNT_ID, MovementTypeEnum.DEPOSIT);
                when(findAccountMovementsUseCase.findMovements(TransactionMother.ACCOUNT_ID))
                        .thenReturn(Flowable.just(movement));

                // Act + Assert
                webTestClient.get().uri("/transactions/accounts/{accountId}/movements", TransactionMother.ACCOUNT_ID)
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
            @DisplayName("findMovements - cuenta sin movimientos - retorna 200 con lista vacia")
            void findMovements_accountWithoutMovements_returns200WithEmptyList() {
                // Arrange
                when(findAccountMovementsUseCase.findMovements(TransactionMother.ACCOUNT_ID))
                        .thenReturn(Flowable.empty());

                // Act + Assert
                webTestClient.get().uri("/transactions/accounts/{accountId}/movements", TransactionMother.ACCOUNT_ID)
                        .exchange()
                        .expectStatus().isOk()
                        .expectBodyList(MovementResponseDto.class)
                        .hasSize(0);
            }
        }

        @Nested
        @Tag("unexpected-case")
        @DisplayName("Casos inesperados")
        class UnexpectedCases {

            @Test
            @DisplayName("findMovements - error no declarado - retorna 500")
            void findMovements_unexpectedError_returns500() {
                // Arrange
                when(findAccountMovementsUseCase.findMovements(TransactionMother.ACCOUNT_ID))
                        .thenReturn(Flowable.error(new RuntimeException("boom")));

                // Act + Assert
                webTestClient.get().uri("/transactions/accounts/{accountId}/movements", TransactionMother.ACCOUNT_ID)
                        .exchange()
                        .expectStatus().is5xxServerError();
            }
        }
    }
}
