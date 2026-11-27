package com.proyecto_final.ms_transaction.infrastructure.adapter.out.client.account;

import com.proyecto_final.ms_transaction.application.port.out.client.account.dto.AccountInfoDto;
import com.proyecto_final.ms_transaction.domain.exception.AccountNotFoundException;
import com.proyecto_final.ms_transaction.domain.model.AccountTypeEnum;
import com.proyecto_final.ms_transaction.infrastructure.adapter.out.client.RemoteProductClient;
import com.proyecto_final.ms_transaction.infrastructure.adapter.out.client.account.dto.AccountClientResponseDto;
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
import org.springframework.web.reactive.function.client.WebClient;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * AccountRestClientAdapter es un adaptador delgado que delega toda la logica HTTP en
 * RemoteProductClient (probado por separado en RemoteProductClientTest); aqui solo se
 * verifica la orquestacion y el mapeo al DTO de dominio.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AccountRestClientAdapter")
class AccountRestClientAdapterTest {

    @Mock
    private RemoteProductClient remoteProductClient;

    @Captor
    private ArgumentCaptor<Supplier<RuntimeException>> notFoundExceptionCaptor;

    private WebClient accountWebClient;
    private AccountRestClientAdapter accountRestClientAdapter;

    @BeforeEach
    void setUp() {
        accountWebClient = WebClient.builder().baseUrl("http://ms-account").build();
        accountRestClientAdapter = new AccountRestClientAdapter(accountWebClient, remoteProductClient);
    }

    @Nested
    @DisplayName("findAccountById")
    class FindAccountById {

        @Nested
        @Tag("success-case")
        @DisplayName("Casos de exito")
        class SuccessCases {

            @Test
            @DisplayName("findAccountById - respuesta del cliente remoto - mapea correctamente al AccountInfoDto")
            void findAccountById_remoteClientResponse_mapsCorrectlyToAccountInfoDto() {
                // Arrange
                AccountClientResponseDto response = new AccountClientResponseDto(
                        "acc-001", "cust-001", AccountTypeEnum.SAVINGS, 10, null);
                when(remoteProductClient.get(eq(accountWebClient), eq("/accounts/{accountId}"), eq("acc-001"),
                        eq(AccountClientResponseDto.class), eq("ms-account"), any()))
                        .thenReturn(Single.just(response));

                // Act
                TestObserver<AccountInfoDto> testObserver = accountRestClientAdapter.findAccountById("acc-001").test();

                // Assert
                testObserver.assertComplete();
                AccountInfoDto result = testObserver.values().get(0);
                assertAll(
                        () -> assertEquals(response.id(), result.id()),
                        () -> assertEquals(response.customerId(), result.customerId()),
                        () -> assertEquals(response.type(), result.type()),
                        () -> assertEquals(response.monthlyMovementLimit(), result.monthlyMovementLimit()),
                        () -> assertEquals(response.movementDayOfMonth(), result.movementDayOfMonth())
                );
            }

            @Test
            @DisplayName("findAccountById - cuenta inexistente en el servicio remoto - delega el 404 con "
                    + "AccountNotFoundException")
            void findAccountById_remoteNotFound_delegatesWithAccountNotFoundExceptionSupplier() {
                // Arrange
                when(remoteProductClient.get(any(), anyString(), anyString(), any(), anyString(),
                        notFoundExceptionCaptor.capture()))
                        .thenReturn(Single.error(new AccountNotFoundException("missing-id")));

                // Act
                TestObserver<AccountInfoDto> testObserver =
                        accountRestClientAdapter.findAccountById("missing-id").test();

                // Assert
                testObserver.assertError(AccountNotFoundException.class);
                RuntimeException builtException = notFoundExceptionCaptor.getValue().get();
                assertInstanceOf(AccountNotFoundException.class, builtException);
                assertEquals("missing-id", ((AccountNotFoundException) builtException).getAccountId());
            }
        }

        @Nested
        @Tag("unexpected-case")
        @DisplayName("Casos inesperados")
        class UnexpectedCases {

            @Test
            @DisplayName("findAccountById - RemoteProductClient falla con error no declarado - propaga el error")
            void findAccountById_remoteProductClientFailsWithUnexpectedError_propagatesError() {
                // Arrange
                RuntimeException unexpectedError = new RuntimeException("unexpected failure");
                when(remoteProductClient.get(any(), anyString(), anyString(), any(), anyString(), any()))
                        .thenReturn(Single.error(unexpectedError));

                // Act
                TestObserver<AccountInfoDto> testObserver = accountRestClientAdapter.findAccountById("acc-001").test();

                // Assert
                testObserver.assertError(unexpectedError);
            }
        }
    }
}
