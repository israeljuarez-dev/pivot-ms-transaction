package com.proyecto_final.ms_transaction.infrastructure.adapter.out.client.credit;

import com.proyecto_final.ms_transaction.application.port.out.client.credit.CreditClientPort;
import com.proyecto_final.ms_transaction.application.port.out.client.credit.dto.CreditInfoDto;
import com.proyecto_final.ms_transaction.domain.exception.CreditNotFoundException;
import com.proyecto_final.ms_transaction.domain.exception.ProductServiceUnavailableException;
import com.proyecto_final.ms_transaction.infrastructure.adapter.out.client.RemoteProductClient;
import com.proyecto_final.ms_transaction.infrastructure.adapter.out.client.credit.dto.CreditClientResponseDto;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import io.reactivex.rxjava3.core.Single;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.concurrent.TimeoutException;

/**
 * Implementa el puerto de salida de créditos consultando la API REST de ms-credit.
 */
@Slf4j
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
    @CircuitBreaker(name = SERVICE_NAME, fallbackMethod = "findCreditByIdFallback")
    @RateLimiter(name = SERVICE_NAME, fallbackMethod = "findCreditByIdFallback")
    @TimeLimiter(name = SERVICE_NAME, fallbackMethod = "findCreditByIdFallback")
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

    private Single<CreditInfoDto> findCreditByIdFallback(String creditId, Throwable throwable) {
        if (throwable instanceof CallNotPermittedException) {
            log.error("Circuit breaker OPEN para ms-credit, id {}", creditId);
            return Single.error(new ProductServiceUnavailableException(SERVICE_NAME, "circuit breaker open"));
        }
        if (throwable instanceof TimeoutException) {
            log.error("ms-credit no respondió dentro del timeout configurado, id {}", creditId);
            return Single.error(new ProductServiceUnavailableException(SERVICE_NAME, "timeout"));
        }

        log.warn("findCreditById fallback, id {}: {}", creditId, throwable.getMessage());
        return Single.error(throwable);
    }
}
