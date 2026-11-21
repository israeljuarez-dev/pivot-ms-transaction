package com.proyecto_final.ms_transaction.application.service;

import com.proyecto_final.ms_transaction.application.port.in.dto.CreditBalanceDto;
import com.proyecto_final.ms_transaction.application.port.in.use_cases.FindCreditBalanceUseCase;
import com.proyecto_final.ms_transaction.application.port.out.client.credit.CreditClientPort;
import com.proyecto_final.ms_transaction.application.port.out.client.credit.dto.CreditInfoDto;
import com.proyecto_final.ms_transaction.application.support.BalanceProvider;
import com.proyecto_final.ms_transaction.domain.model.ProductBalance;
import com.proyecto_final.ms_transaction.domain.model.ProductCategoryEnum;
import io.reactivex.rxjava3.core.Single;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Implementa el caso de uso FindCreditBalanceUseCase: orquesta las reglas del negocio.
 */
@Service
@RequiredArgsConstructor
public class FindCreditBalanceService implements FindCreditBalanceUseCase {

    private final CreditClientPort creditClientPort;

    private final BalanceProvider balanceProvider;

    /**
     * {@inheritDoc}
     */
    @Override
    public Single<CreditBalanceDto> findBalance(String creditId) {
        return creditClientPort.findCreditById(creditId)
                .flatMap(this::loadBalance);
    }

    /**
     * Carga la deuda del crédito y calcula el crédito disponible.
     */
    private Single<CreditBalanceDto> loadBalance(CreditInfoDto credit) {
        return balanceProvider
                .load(ProductCategoryEnum.CREDIT, credit.id(), credit.type().initialDebt(credit.approvedAmount()))
                .map(balance -> CreditBalanceDto.builder()
                        .creditId(credit.id())
                        .creditType(credit.type())
                        .outstandingDebt(balance.getAmount())
                        .availableCredit(calculateAvailableCredit(credit, balance))
                        .build());
    }

    /**
     * Crédito disponible = límite - consumido. Los préstamos no tienen límite, devuelven null.
     */
    private BigDecimal calculateAvailableCredit(CreditInfoDto credit, ProductBalance balance) {
        if (credit.creditLimit() == null) {
            return null;
        }

        return credit.creditLimit().subtract(balance.getAmount());
    }
}
