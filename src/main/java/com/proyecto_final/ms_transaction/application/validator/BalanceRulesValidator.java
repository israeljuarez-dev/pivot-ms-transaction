package com.proyecto_final.ms_transaction.application.validator;

import com.proyecto_final.ms_transaction.application.port.out.client.credit.dto.CreditInfoDto;
import com.proyecto_final.ms_transaction.domain.exception.ConsumptionNotAllowedException;
import com.proyecto_final.ms_transaction.domain.exception.CreditLimitExceededException;
import com.proyecto_final.ms_transaction.domain.exception.InsufficientFundsException;
import com.proyecto_final.ms_transaction.domain.exception.PaymentExceedsDebtException;
import com.proyecto_final.ms_transaction.domain.model.ProductBalance;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Aplica las reglas que protegen los saldos: fondos suficientes, pagos dentro de la deuda
 * y consumos dentro del límite de la tarjeta.
 */
@Slf4j
@Component
public class BalanceRulesValidator {

    /**
     * Verifica que la cuenta tenga fondos suficientes para el retiro.
     */
    public void validateSufficientFunds(ProductBalance balance, BigDecimal amount, String accountId) {
        if (!balance.covers(amount)) {
            log.warn("Withdrawal rejected, insufficient funds in account {}", accountId);
            throw new InsufficientFundsException(accountId, balance.getAmount(), amount);
        }
    }

    /**
     * Verifica que el pago no supere la deuda pendiente.
     */
    public void validatePaymentWithinDebt(ProductBalance balance, BigDecimal amount, String creditId) {
        if (!balance.covers(amount)) {
            log.warn("Payment rejected, amount exceeds the outstanding debt of credit {}", creditId);
            throw new PaymentExceedsDebtException(creditId, balance.getAmount(), amount);
        }
    }

    /**
     * Verifica que el crédito sea una tarjeta, único tipo que admite consumos.
     */
    public void validateConsumptionAllowed(CreditInfoDto credit) {
        if (!credit.type().allowsConsumption()) {
            log.warn("Consumption rejected, credit {} is a {}", credit.id(), credit.type());
            throw new ConsumptionNotAllowedException(credit.id(), credit.type());
        }
    }

    /**
     * Verifica que el consumo no haga superar el límite de crédito de la tarjeta.
     */
    public void validateWithinCreditLimit(ProductBalance balance, BigDecimal amount, CreditInfoDto credit) {
        if (!balance.fitsWithin(amount, credit.creditLimit())) {
            log.warn("Consumption rejected, amount exceeds the credit limit of credit {}", credit.id());
            throw new CreditLimitExceededException(
                    credit.id(), credit.creditLimit(), balance.getAmount(), amount);
        }
    }
}
