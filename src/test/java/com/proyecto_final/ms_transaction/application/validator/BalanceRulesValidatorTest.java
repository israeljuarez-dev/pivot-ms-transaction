package com.proyecto_final.ms_transaction.application.validator;

import com.proyecto_final.ms_transaction.application.port.out.client.credit.dto.CreditInfoDto;
import com.proyecto_final.ms_transaction.domain.exception.ConsumptionNotAllowedException;
import com.proyecto_final.ms_transaction.domain.exception.CreditLimitExceededException;
import com.proyecto_final.ms_transaction.domain.exception.InsufficientFundsException;
import com.proyecto_final.ms_transaction.domain.exception.PaymentExceedsDebtException;
import com.proyecto_final.ms_transaction.domain.model.ProductBalance;
import com.proyecto_final.ms_transaction.application.support.TransactionMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * BalanceRulesValidator no tiene dependencias externas: se prueba directamente, sin mocks.
 */
@DisplayName("BalanceRulesValidator")
class BalanceRulesValidatorTest {

    private final BalanceRulesValidator validator = new BalanceRulesValidator();

    @Nested
    @DisplayName("validateSufficientFunds")
    class ValidateSufficientFunds {

        @Nested
        @Tag("success-case")
        @DisplayName("Casos de exito")
        class SuccessCases {

            @Test
            @DisplayName("validateSufficientFunds - saldo mayor al monto - no lanza excepcion")
            void validateSufficientFunds_balanceGreaterThanAmount_doesNotThrow() {
                // Arrange
                ProductBalance balance = TransactionMother.accountBalance(BigDecimal.valueOf(500));

                // Act + Assert
                assertDoesNotThrow(() ->
                        validator.validateSufficientFunds(balance, BigDecimal.valueOf(100), "acc-001"));
            }
        }

        @Nested
        @Tag("rare-case")
        @DisplayName("Casos poco frecuentes")
        class RareCases {

            @Test
            @DisplayName("validateSufficientFunds - retiro exactamente igual al saldo disponible - no lanza excepcion")
            void validateSufficientFunds_amountEqualsBalance_doesNotThrow() {
                // Arrange
                ProductBalance balance = TransactionMother.accountBalance(BigDecimal.valueOf(100));

                // Act + Assert
                assertDoesNotThrow(() ->
                        validator.validateSufficientFunds(balance, BigDecimal.valueOf(100), "acc-001"));
            }
        }

        @Nested
        @Tag("error-case")
        @DisplayName("Casos de error")
        class ErrorCases {

            @Test
            @DisplayName("validateSufficientFunds - saldo menor al monto - lanza InsufficientFundsException")
            void validateSufficientFunds_balanceLowerThanAmount_throwsInsufficientFundsException() {
                // Arrange
                ProductBalance balance = TransactionMother.accountBalance(BigDecimal.valueOf(50));
                BigDecimal requestedAmount = BigDecimal.valueOf(100);
                String accountId = "acc-001";

                // Act
                InsufficientFundsException error = assertThrows(
                        InsufficientFundsException.class,
                        () -> validator.validateSufficientFunds(balance, requestedAmount, accountId)
                );

                // Assert
                assertAll(
                        () -> assertEquals(accountId, error.getAccountId()),
                        () -> assertEquals(BigDecimal.valueOf(50), error.getAvailableBalance()),
                        () -> assertEquals(requestedAmount, error.getRequestedAmount())
                );
            }
        }
    }

    @Nested
    @DisplayName("validatePaymentWithinDebt")
    class ValidatePaymentWithinDebt {

        @Nested
        @Tag("success-case")
        @DisplayName("Casos de exito")
        class SuccessCases {

            @Test
            @DisplayName("validatePaymentWithinDebt - pago menor a la deuda - no lanza excepcion")
            void validatePaymentWithinDebt_paymentLowerThanDebt_doesNotThrow() {
                // Arrange
                ProductBalance balance = TransactionMother.creditBalance(BigDecimal.valueOf(500));

                // Act + Assert
                assertDoesNotThrow(() ->
                        validator.validatePaymentWithinDebt(balance, BigDecimal.valueOf(200), "cred-001"));
            }
        }

        @Nested
        @Tag("error-case")
        @DisplayName("Casos de error")
        class ErrorCases {

            @Test
            @DisplayName("validatePaymentWithinDebt - pago mayor a la deuda - lanza PaymentExceedsDebtException")
            void validatePaymentWithinDebt_paymentGreaterThanDebt_throwsPaymentExceedsDebtException() {
                // Arrange
                ProductBalance balance = TransactionMother.creditBalance(BigDecimal.valueOf(100));
                BigDecimal requestedAmount = BigDecimal.valueOf(200);
                String creditId = "cred-001";

                // Act
                PaymentExceedsDebtException error = assertThrows(
                        PaymentExceedsDebtException.class,
                        () -> validator.validatePaymentWithinDebt(balance, requestedAmount, creditId)
                );

                // Assert
                assertAll(
                        () -> assertEquals(creditId, error.getCreditId()),
                        () -> assertEquals(BigDecimal.valueOf(100), error.getOutstandingDebt()),
                        () -> assertEquals(requestedAmount, error.getRequestedAmount())
                );
            }
        }
    }

    @Nested
    @DisplayName("validateConsumptionAllowed")
    class ValidateConsumptionAllowed {

        @Nested
        @Tag("success-case")
        @DisplayName("Casos de exito")
        class SuccessCases {

            @Test
            @DisplayName("validateConsumptionAllowed - tarjeta de credito - no lanza excepcion")
            void validateConsumptionAllowed_creditCard_doesNotThrow() {
                // Arrange
                CreditInfoDto credit = TransactionMother.creditCard();

                // Act + Assert
                assertDoesNotThrow(() -> validator.validateConsumptionAllowed(credit));
            }
        }

        @Nested
        @Tag("error-case")
        @DisplayName("Casos de error")
        class ErrorCases {

            @Test
            @DisplayName("validateConsumptionAllowed - prestamo - lanza ConsumptionNotAllowedException")
            void validateConsumptionAllowed_loan_throwsConsumptionNotAllowedException() {
                // Arrange
                CreditInfoDto credit = TransactionMother.loan();

                // Act
                ConsumptionNotAllowedException error = assertThrows(
                        ConsumptionNotAllowedException.class, () -> validator.validateConsumptionAllowed(credit));

                // Assert
                assertAll(
                        () -> assertEquals(credit.id(), error.getCreditId()),
                        () -> assertEquals(credit.type(), error.getCreditType())
                );
            }
        }
    }

    @Nested
    @DisplayName("validateWithinCreditLimit")
    class ValidateWithinCreditLimit {

        @Nested
        @Tag("success-case")
        @DisplayName("Casos de exito")
        class SuccessCases {

            @Test
            @DisplayName("validateWithinCreditLimit - consumo dentro del limite - no lanza excepcion")
            void validateWithinCreditLimit_consumptionWithinLimit_doesNotThrow() {
                // Arrange
                CreditInfoDto credit = TransactionMother.creditCard();
                ProductBalance balance = TransactionMother.creditBalance(BigDecimal.valueOf(200));

                // Act + Assert
                assertDoesNotThrow(() ->
                        validator.validateWithinCreditLimit(balance, BigDecimal.valueOf(100), credit));
            }
        }

        @Nested
        @Tag("error-case")
        @DisplayName("Casos de error")
        class ErrorCases {

            @Test
            @DisplayName("validateWithinCreditLimit - consumo supera el limite - lanza CreditLimitExceededException")
            void validateWithinCreditLimit_consumptionExceedsLimit_throwsCreditLimitExceededException() {
                // Arrange
                CreditInfoDto credit = TransactionMother.creditCard();
                ProductBalance balance = TransactionMother.creditBalance(BigDecimal.valueOf(950));
                BigDecimal requestedAmount = BigDecimal.valueOf(100);
                String creditId = credit.id();
                BigDecimal creditLimit = credit.creditLimit();

                // Act
                CreditLimitExceededException error = assertThrows(
                        CreditLimitExceededException.class,
                        () -> validator.validateWithinCreditLimit(balance, requestedAmount, credit)
                );

                // Assert
                assertAll(
                        () -> assertEquals(creditId, error.getCreditId()),
                        () -> assertEquals(creditLimit, error.getCreditLimit()),
                        () -> assertEquals(BigDecimal.valueOf(950), error.getConsumedAmount()),
                        () -> assertEquals(requestedAmount, error.getRequestedAmount())
                );
            }
        }
    }
}