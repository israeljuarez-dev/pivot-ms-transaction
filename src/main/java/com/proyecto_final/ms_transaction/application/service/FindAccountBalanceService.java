package com.proyecto_final.ms_transaction.application.service;

import com.proyecto_final.ms_transaction.application.port.in.dto.AccountBalanceDto;
import com.proyecto_final.ms_transaction.application.port.in.use_cases.FindAccountBalanceUseCase;
import com.proyecto_final.ms_transaction.application.port.out.client.account.AccountClientPort;
import com.proyecto_final.ms_transaction.application.port.out.client.account.dto.AccountInfoDto;
import com.proyecto_final.ms_transaction.application.support.BalanceProvider;
import com.proyecto_final.ms_transaction.domain.model.ProductCategoryEnum;
import io.reactivex.rxjava3.core.Single;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Implementa el caso de uso FindAccountBalanceUseCase: orquesta las reglas del negocio.
 */
@Service
@RequiredArgsConstructor
public class FindAccountBalanceService implements FindAccountBalanceUseCase {

    private final AccountClientPort accountClientPort;

    private final BalanceProvider balanceProvider;

    /**
     * {@inheritDoc}
     */
    @Override
    public Single<AccountBalanceDto> findBalance(String accountId) {
        return accountClientPort.findAccountById(accountId)
                .flatMap(this::loadBalance);
    }

    /**
     * Carga el saldo de la cuenta (cero si aún no tiene movimientos) y lo arma como respuesta.
     */
    private Single<AccountBalanceDto> loadBalance(AccountInfoDto account) {
        return balanceProvider.load(ProductCategoryEnum.ACCOUNT, account.id(), BigDecimal.ZERO)
                .map(balance -> AccountBalanceDto.builder()
                        .accountId(account.id())
                        .accountType(account.type())
                        .balance(balance.getAmount())
                        .build()
                );
    }
}