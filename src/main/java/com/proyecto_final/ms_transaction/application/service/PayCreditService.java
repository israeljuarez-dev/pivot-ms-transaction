package com.proyecto_final.ms_transaction.application.service;

import com.proyecto_final.ms_transaction.application.port.in.dto.CreditOperationDto;
import com.proyecto_final.ms_transaction.application.port.in.use_cases.PayCreditUseCase;
import com.proyecto_final.ms_transaction.application.port.out.client.credit.CreditClientPort;
import com.proyecto_final.ms_transaction.application.port.out.client.credit.dto.CreditInfoDto;
import com.proyecto_final.ms_transaction.application.support.BalanceProvider;
import com.proyecto_final.ms_transaction.application.support.MovementRecorder;
import com.proyecto_final.ms_transaction.application.validator.BalanceRulesValidator;
import com.proyecto_final.ms_transaction.domain.model.Movement;
import com.proyecto_final.ms_transaction.domain.model.MovementTypeEnum;
import com.proyecto_final.ms_transaction.domain.model.ProductBalance;
import com.proyecto_final.ms_transaction.domain.model.ProductCategoryEnum;
import io.reactivex.rxjava3.core.Single;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;

/**
 * Implementa el caso de uso PayCreditUseCase: orquesta las reglas del negocio.
 */
@Service
@RequiredArgsConstructor
public class PayCreditService implements PayCreditUseCase {

    private final CreditClientPort creditClientPort;

    private final BalanceProvider balanceProvider;

    private final MovementRecorder movementRecorder;

    private final BalanceRulesValidator balanceRulesValidator;

    private final Clock clock;

    /**
     * {@inheritDoc}
     */
    @Override
    public Single<Movement> pay(CreditOperationDto creditOperationDto) {
        return creditClientPort.findCreditById(creditOperationDto.creditId())
                .flatMap(credit -> loadAndPay(credit, creditOperationDto))
                .flatMap(updatedBalance -> movementRecorder.record(
                        updatedBalance,
                        MovementTypeEnum.PAYMENT,
                        creditOperationDto.amount(),
                        creditOperationDto.description()));
    }

    /**
     * Carga la deuda actual del crédito y le resta el pago, validando que no la supere.
     */
    private Single<ProductBalance> loadAndPay(CreditInfoDto credit, CreditOperationDto creditOperationDto) {
        return balanceProvider.load(
                        ProductCategoryEnum.CREDIT,
                        credit.id(),
                        credit.type().initialDebt(credit.approvedAmount())
                )
                .map(balance -> {
                    balanceRulesValidator.validatePaymentWithinDebt(
                            balance,
                            creditOperationDto.amount(),
                            credit.id()
                    );
                    return balance.subtract(creditOperationDto.amount(), clock.instant());
                });
    }
}
