package com.proyecto_final.ms_transaction.infrastructure.adapter.out.client.account;

import com.proyecto_final.ms_transaction.application.port.out.client.account.AccountClientPort;
import com.proyecto_final.ms_transaction.application.port.out.client.account.dto.AccountInfoDto;
import com.proyecto_final.ms_transaction.domain.exception.AccountNotFoundException;
import com.proyecto_final.ms_transaction.infrastructure.adapter.out.client.RemoteProductClient;
import com.proyecto_final.ms_transaction.infrastructure.adapter.out.client.account.dto.AccountClientResponseDto;
import io.reactivex.rxjava3.core.Single;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Implementa el puerto de salida de cuentas consultando la API REST de ms-account.
 */
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
}
