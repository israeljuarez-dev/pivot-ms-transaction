package com.proyecto_final.ms_transaction.infrastructure.adapter.out.client.credit;

import com.proyecto_final.ms_transaction.application.port.out.client.credit.dto.CreditInfoDto;
import com.proyecto_final.ms_transaction.domain.exception.CreditNotFoundException;
import com.proyecto_final.ms_transaction.domain.model.CreditTypeEnum;
import com.proyecto_final.ms_transaction.infrastructure.adapter.out.client.RemoteProductClient;
import com.proyecto_final.ms_transaction.infrastructure.adapter.out.client.credit.dto.CreditClientResponseDto;
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

import java.math.BigDecimal;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreditRestClientAdapter")
class CreditRestClientAdapterTest {

    @Mock
    private RemoteProductClient remoteProductClient;

    @Captor
    private ArgumentCaptor<Supplier<RuntimeException>> notFoundExceptionCaptor;

    private WebClient creditWebClient;
    private CreditRestClientAdapter creditRestClientAdapter;

    @BeforeEach
    void setUp() {
        creditWebClient = WebClient.builder().baseUrl("http://ms-credit").build();
        creditRestClientAdapter = new CreditRestClientAdapter(creditWebClient, remoteProductClient);
    }

    @Nested
    @DisplayName("findCreditById")
    class FindCreditById {

        @Nested
        @Tag("success-case")
        @DisplayName("Casos de exito")
        class SuccessCases {

            @Test
            @DisplayName("findCreditById - respuesta del cliente remoto - mapea correctamente al CreditInfoDto")
            void findCreditById_remoteClientResponse_mapsCorrectlyToCreditInfoDto() {
                // Arrange
                CreditClientResponseDto response = new CreditClientResponseDto(
                        "cred-001", "cust-001", CreditTypeEnum.CREDIT_CARD, null, BigDecimal.valueOf(1000));
                when(remoteProductClient.get(eq(creditWebClient), eq("/credits/{creditId}"), eq("cred-001"),
                        eq(CreditClientResponseDto.class), eq("ms-credit"), any()))
                        .thenReturn(Single.just(response));

                // Act
                TestObserver<CreditInfoDto> testObserver = creditRestClientAdapter.findCreditById("cred-001").test();

                // Assert
                testObserver.assertComplete();
                CreditInfoDto result = testObserver.values().get(0);
                assertAll(
                        () -> assertEquals(response.id(), result.id()),
                        () -> assertEquals(response.customerId(), result.customerId()),
                        () -> assertEquals(response.type(), result.type()),
                        () -> assertEquals(response.approvedAmount(), result.approvedAmount()),
                        () -> assertEquals(response.creditLimit(), result.creditLimit())
                );
            }

            @Test
            @DisplayName("findCreditById - credito inexistente en el servicio remoto - delega el 404 con "
                    + "CreditNotFoundException")
            void findCreditById_remoteNotFound_delegatesWithCreditNotFoundExceptionSupplier() {
                // Arrange
                when(remoteProductClient.get(any(), anyString(), anyString(), any(), anyString(),
                        notFoundExceptionCaptor.capture()))
                        .thenReturn(Single.error(new CreditNotFoundException("missing-id")));

                // Act
                TestObserver<CreditInfoDto> testObserver = creditRestClientAdapter.findCreditById("missing-id").test();

                // Assert
                testObserver.assertError(CreditNotFoundException.class);
                RuntimeException builtException = notFoundExceptionCaptor.getValue().get();
                assertInstanceOf(CreditNotFoundException.class, builtException);
                assertEquals("missing-id", ((CreditNotFoundException) builtException).getCreditId());
            }
        }

        @Nested
        @Tag("unexpected-case")
        @DisplayName("Casos inesperados")
        class UnexpectedCases {

            @Test
            @DisplayName("findCreditById - RemoteProductClient falla con error no declarado - propaga el error")
            void findCreditById_remoteProductClientFailsWithUnexpectedError_propagatesError() {
                // Arrange
                RuntimeException unexpectedError = new RuntimeException("unexpected failure");
                when(remoteProductClient.get(any(), anyString(), anyString(), any(), anyString(), any()))
                        .thenReturn(Single.error(unexpectedError));

                // Act
                TestObserver<CreditInfoDto> testObserver = creditRestClientAdapter.findCreditById("cred-001").test();

                // Assert
                testObserver.assertError(unexpectedError);
            }
        }
    }
}