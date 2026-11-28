package com.proyecto_final.ms_transaction.infrastructure.adapter.out.client.account;

import com.proyecto_final.ms_transaction.application.port.out.client.account.AccountClientPort;
import com.proyecto_final.ms_transaction.application.port.out.client.account.dto.AccountInfoDto;
import com.proyecto_final.ms_transaction.domain.exception.AccountNotFoundException;
import com.proyecto_final.ms_transaction.domain.exception.ProductServiceUnavailableException;
import com.proyecto_final.ms_transaction.infrastructure.adapter.out.client.RemoteProductClient;
import com.proyecto_final.ms_transaction.infrastructure.adapter.out.client.account.dto.AccountClientResponseDto;
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
 * Implementa el puerto de salida de cuentas consultando la API REST de ms-account.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccountRestClientAdapter implements AccountClientPort {

    private static final String SERVICE_NAME = "ms-account";

    private static final String ACCOUNT_URI = "/accounts/{accountId}";

    private final WebClient accountWebClient;

    private final RemoteProductClient remoteProductClient;

    /**
     * {@inheritDoc}
     */
    @CircuitBreaker(name = SERVICE_NAME, fallbackMethod = "findAccountByIdFallback")
    @RateLimiter(name = SERVICE_NAME, fallbackMethod = "findAccountByIdFallback")
    @TimeLimiter(name = SERVICE_NAME, fallbackMethod = "findAccountByIdFallback")
    @Override
    public Single<AccountInfoDto> findAccountById(String accountId) {
        return remoteProductClient.get(
                        accountWebClient,
                        ACCOUNT_URI,
                        accountId,
                        AccountClientResponseDto.class,
                        SERVICE_NAME,
                        () -> new AccountNotFoundException(accountId)
                )
                .map(response -> new AccountInfoDto(
                        response.id(),
                        response.customerId(),
                        response.type(),
                        response.monthlyMovementLimit(),
                        response.movementDayOfMonth()));
    }

    private Single<AccountInfoDto> findAccountByIdFallback(String accountId, Throwable throwable) {
        if (throwable instanceof CallNotPermittedException) {
            log.error("Circuit breaker OPEN para ms-account, id {}", accountId);
            return Single.error(new ProductServiceUnavailableException(SERVICE_NAME, "circuit breaker open"));
        }
        if (throwable instanceof TimeoutException) {
            log.error("ms-account no respondió dentro del timeout configurado, id {}", accountId);
            return Single.error(new ProductServiceUnavailableException(SERVICE_NAME, "timeout"));
        }

        log.warn("findAccountById fallback, id {}: {}", accountId, throwable.getMessage());
        return Single.error(throwable);
    }
}
