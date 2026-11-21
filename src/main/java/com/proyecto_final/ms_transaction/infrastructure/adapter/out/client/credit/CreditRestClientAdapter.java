package com.proyecto_final.ms_transaction.infrastructure.adapter.out.client.credit;

import com.proyecto_final.ms_transaction.application.port.out.client.credit.CreditClientPort;
import com.proyecto_final.ms_transaction.application.port.out.client.credit.dto.CreditInfoDto;
import com.proyecto_final.ms_transaction.domain.exception.CreditNotFoundException;
import com.proyecto_final.ms_transaction.infrastructure.adapter.out.client.RemoteProductClient;
import com.proyecto_final.ms_transaction.infrastructure.adapter.out.client.credit.dto.CreditClientResponseDto;
import io.reactivex.rxjava3.core.Single;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Implementa el puerto de salida de créditos consultando la API REST de ms-credit.
 */
@Component
@RequiredArgsConstructor
public class CreditRestClientAdapter implements CreditClientPort {

    private static final String SERVICE_NAME = "ms-credit";

    private static final String CREDIT_URI = "/credits/{creditId}";

    private final WebClient creditWebClient;

    private final RemoteProductClient remoteProductClient;

    /**
     * {@inheritDoc}
     */
    @Override
    public Single<CreditInfoDto> findCreditById(String creditId) {
        return remoteProductClient.get(
                        creditWebClient,
                        CREDIT_URI,
                        creditId,
                        CreditClientResponseDto.class,
                        SERVICE_NAME,
                        () -> new CreditNotFoundException(creditId)
                )
                .map(response -> new CreditInfoDto(
                        response.id(),
                        response.customerId(),
                        response.type(),
                        response.approvedAmount(),
                        response.creditLimit())
                );
    }
}
