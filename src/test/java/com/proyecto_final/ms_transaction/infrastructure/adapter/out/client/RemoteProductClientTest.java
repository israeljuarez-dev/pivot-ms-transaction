package com.proyecto_final.ms_transaction.infrastructure.adapter.out.client;

import com.proyecto_final.ms_transaction.domain.exception.AccountNotFoundException;
import com.proyecto_final.ms_transaction.domain.exception.ProductServiceUnavailableException;
import com.proyecto_final.ms_transaction.infrastructure.adapter.out.client.account.dto.AccountClientResponseDto;
import io.reactivex.rxjava3.observers.TestObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * RemoteProductClient usa WebClient directamente: la frontera HTTP se simula mockeando
 * ExchangeFunction (en vez de levantar un servidor real), siguiendo el mismo principio
 * de WebTestClient.bindToController pero para el lado cliente.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RemoteProductClient")
class RemoteProductClientTest {

    private static final String URI_TEMPLATE = "/accounts/{accountId}";
    private static final String PRODUCT_ID = "acc-001";
    private static final String SERVICE_NAME = "ms-account";

    @Mock
    private ExchangeFunction exchangeFunction;

    private RemoteProductClient remoteProductClient;
    private WebClient webClient;

    @BeforeEach
    void setUp() {
        remoteProductClient = new RemoteProductClient();
        webClient = WebClient.builder().baseUrl("http://ms-account").exchangeFunction(exchangeFunction).build();
    }

    @Nested
    @DisplayName("get")
    class Get {

        @Nested
        @Tag("success-case")
        @DisplayName("Casos de exito")
        class SuccessCases {

            @Test
            @DisplayName("get - el servicio responde 200 - retorna el cuerpo mapeado al tipo pedido")
            void get_serviceRespondsOk_returnsBodyMappedToRequestedType() {
                // Arrange
                AccountClientResponseDto body = new AccountClientResponseDto(
                        PRODUCT_ID, "cust-001", com.proyecto_final.ms_transaction.domain.model.AccountTypeEnum.SAVINGS,
                        10, null);
                when(exchangeFunction.exchange(any(ClientRequest.class))).thenReturn(Mono.just(
                        ClientResponse.create(HttpStatus.OK)
                                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .body("""
                                        {"id":"acc-001","customerId":"cust-001","type":"SAVINGS",
                                        "monthlyMovementLimit":10,"movementDayOfMonth":null}
                                        """)
                                .build()));

                // Act
                TestObserver<AccountClientResponseDto> testObserver = remoteProductClient.get(
                        webClient, URI_TEMPLATE, PRODUCT_ID, AccountClientResponseDto.class, SERVICE_NAME,
                        () -> new AccountNotFoundException(PRODUCT_ID)).test();

                // Assert
                testObserver.assertComplete();
                AccountClientResponseDto result = testObserver.values().get(0);
                assertAll(
                        () -> assertEquals(body.id(), result.id()),
                        () -> assertEquals(body.customerId(), result.customerId()),
                        () -> assertEquals(body.type(), result.type())
                );
            }
        }

        @Nested
        @Tag("error-case")
        @DisplayName("Casos de error")
        class ErrorCases {

            @Test
            @DisplayName("get - el servicio responde 404 - lanza la excepcion provista por el supplier")
            void get_serviceRespondsNotFound_throwsExceptionFromSupplier() {
                // Arrange
                when(exchangeFunction.exchange(any(ClientRequest.class))).thenReturn(Mono.just(
                        ClientResponse.create(HttpStatus.NOT_FOUND).build()));

                // Act
                TestObserver<AccountClientResponseDto> testObserver = remoteProductClient.get(
                        webClient, URI_TEMPLATE, PRODUCT_ID, AccountClientResponseDto.class, SERVICE_NAME,
                        () -> new AccountNotFoundException(PRODUCT_ID)).test();

                // Assert
                testObserver.assertError(error ->
                        error instanceof AccountNotFoundException
                                && ((AccountNotFoundException) error).getAccountId().equals(PRODUCT_ID)
                );
            }

            @Test
            @DisplayName("get - el servicio responde 500 - lanza ProductServiceUnavailableException")
            void get_serviceRespondsServerError_throwsProductServiceUnavailableException() {
                // Arrange
                when(exchangeFunction.exchange(any(ClientRequest.class))).thenReturn(Mono.just(
                        ClientResponse.create(HttpStatus.INTERNAL_SERVER_ERROR).build()));

                // Act
                TestObserver<AccountClientResponseDto> testObserver = remoteProductClient.get(
                        webClient, URI_TEMPLATE, PRODUCT_ID, AccountClientResponseDto.class, SERVICE_NAME,
                        () -> new AccountNotFoundException(PRODUCT_ID)).test();

                // Assert
                testObserver.assertError(error ->
                        error instanceof ProductServiceUnavailableException
                                && ((ProductServiceUnavailableException) error).getServiceName().equals(SERVICE_NAME)
                );
            }
        }

        @Nested
        @Tag("unexpected-case")
        @DisplayName("Casos inesperados")
        class UnexpectedCases {

            @Test
            @DisplayName("get - el servicio no es alcanzable - lanza ProductServiceUnavailableException")
            void get_serviceUnreachable_throwsProductServiceUnavailableException() {
                // Arrange
                WebClientRequestException networkError = new WebClientRequestException(
                        new RuntimeException("Connection refused"), org.springframework.http.HttpMethod.GET,
                        URI.create("http://ms-account/accounts/" + PRODUCT_ID), org.springframework.http.HttpHeaders.EMPTY);
                when(exchangeFunction.exchange(any(ClientRequest.class))).thenReturn(Mono.error(networkError));

                // Act
                TestObserver<AccountClientResponseDto> testObserver = remoteProductClient.get(
                        webClient, URI_TEMPLATE, PRODUCT_ID, AccountClientResponseDto.class, SERVICE_NAME,
                        () -> new AccountNotFoundException(PRODUCT_ID)).test();

                // Assert
                testObserver.assertError(error ->
                        error instanceof ProductServiceUnavailableException
                                && ((ProductServiceUnavailableException) error).getServiceName().equals(SERVICE_NAME)
                );
            }
        }
    }
}
