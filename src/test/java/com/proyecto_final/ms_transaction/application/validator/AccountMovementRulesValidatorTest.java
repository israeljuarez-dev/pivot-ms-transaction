package com.proyecto_final.ms_transaction.application.validator;

import com.proyecto_final.ms_transaction.application.port.out.client.account.dto.AccountInfoDto;
import com.proyecto_final.ms_transaction.domain.exception.MonthlyMovementLimitExceededException;
import com.proyecto_final.ms_transaction.domain.exception.MovementDayNotAllowedException;
import com.proyecto_final.ms_transaction.domain.model.AccountTypeEnum;
import com.proyecto_final.ms_transaction.application.support.TransactionMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * AccountMovementRulesValidator no tiene dependencias externas: se prueba directamente,
 * sin mocks, instanciandola con "new".
 */
@DisplayName("AccountMovementRulesValidator")
class AccountMovementRulesValidatorTest {

    private final AccountMovementRulesValidator validator = new AccountMovementRulesValidator();

    @Nested
    @DisplayName("validateMovementDay")
    class ValidateMovementDay {

        @Nested
        @Tag("success-case")
        @DisplayName("Casos de exito")
        class SuccessCases {

            @Test
            @DisplayName("validateMovementDay - cuenta de ahorro en cualquier dia - no lanza excepcion")
            void validateMovementDay_savingsAccountAnyDay_doesNotThrow() {
                // Arrange
                AccountInfoDto account = TransactionMother.savingsAccount();

                // Act + Assert
                assertDoesNotThrow(() -> validator.validateMovementDay(account, 25));
            }

            @Test
            @DisplayName("validateMovementDay - plazo fijo en el dia permitido - no lanza excepcion")
            void validateMovementDay_fixedTermOnAllowedDay_doesNotThrow() {
                // Arrange
                AccountInfoDto account = TransactionMother.fixedTermAccount(15);

                // Act + Assert
                assertDoesNotThrow(() -> validator.validateMovementDay(account, 15));
            }
        }

        @Nested
        @Tag("error-case")
        @DisplayName("Casos de error")
        class ErrorCases {

            @Test
            @DisplayName("validateMovementDay - plazo fijo en dia distinto al permitido - lanza MovementDayNotAllowedException")
            void validateMovementDay_fixedTermOnWrongDay_throwsMovementDayNotAllowedException() {
                // Arrange
                AccountInfoDto account = TransactionMother.fixedTermAccount(15);

                // Act
                MovementDayNotAllowedException error = assertThrows(
                        MovementDayNotAllowedException.class, () -> validator.validateMovementDay(account, 20));

                // Assert
                assertAll(
                        () -> assertEquals(account.id(), error.getAccountId()),
                        () -> assertEquals(15, error.getAllowedDayOfMonth())
                );
            }
        }

        @Nested
        @Tag("rare-case")
        @DisplayName("Casos poco frecuentes")
        class RareCases {

            @Test
            @DisplayName("validateMovementDay - plazo fijo sin dia configurado - lanza MovementDayNotAllowedException")
            void validateMovementDay_fixedTermWithoutConfiguredDay_throwsMovementDayNotAllowedException() {
                // Arrange
                AccountInfoDto account = new AccountInfoDto(
                        "acc-004", "cust-004",
                        AccountTypeEnum.FIXED_TERM, null, null);

                // Act + Assert
                assertThrows(MovementDayNotAllowedException.class, () -> validator.validateMovementDay(account, 1));
            }
        }
    }

    @Nested
    @DisplayName("validateMonthlyLimit")
    class ValidateMonthlyLimit {

        @Nested
        @Tag("success-case")
        @DisplayName("Casos de exito")
        class SuccessCases {

            @Test
            @DisplayName("validateMonthlyLimit - ahorro por debajo del limite - no lanza excepcion")
            void validateMonthlyLimit_savingsBelowLimit_doesNotThrow() {
                // Arrange
                AccountInfoDto account = TransactionMother.savingsAccount();

                // Act + Assert
                assertDoesNotThrow(() -> validator.validateMonthlyLimit(account, 5));
            }
        }

        @Nested
        @Tag("error-case")
        @DisplayName("Casos de error")
        class ErrorCases {

            @Test
            @DisplayName("validateMonthlyLimit - ahorro alcanzo el limite - lanza MonthlyMovementLimitExceededException")
            void validateMonthlyLimit_savingsReachedLimit_throwsMonthlyMovementLimitExceededException() {
                // Arrange
                AccountInfoDto account = TransactionMother.savingsAccount();
                long monthlyLimit = account.monthlyMovementLimit();

                // Act
                MonthlyMovementLimitExceededException error = assertThrows(
                        MonthlyMovementLimitExceededException.class,
                        () -> validator.validateMonthlyLimit(account, monthlyLimit)
                );

                // Assert
                assertAll(
                        () -> assertEquals(account.id(), error.getAccountId()),
                        () -> assertEquals(account.type(), error.getAccountType())
                );
            }

            @Test
            @DisplayName("validateMonthlyLimit - plazo fijo ya tiene un movimiento - lanza MonthlyMovementLimitExceededException")
            void validateMonthlyLimit_fixedTermAlreadyHasOneMovement_throwsMonthlyMovementLimitExceededException() {
                // Arrange
                AccountInfoDto account = TransactionMother.fixedTermAccount(10);

                // Act + Assert
                assertThrows(MonthlyMovementLimitExceededException.class,
                        () -> validator.validateMonthlyLimit(account, 1));
            }
        }

        @Nested
        @Tag("rare-case")
        @DisplayName("Casos poco frecuentes")
        class RareCases {

            @Test
            @DisplayName("validateMonthlyLimit - cuenta corriente sin limite - nunca lanza excepcion")
            void validateMonthlyLimit_checkingAccount_neverThrows() {
                // Arrange
                AccountInfoDto account = TransactionMother.checkingAccount();

                // Act + Assert
                assertDoesNotThrow(() -> validator.validateMonthlyLimit(account, 10_000));
            }

            @Test
            @DisplayName("validateMonthlyLimit - ahorro sin limite configurado - no lanza excepcion")
            void validateMonthlyLimit_savingsWithoutConfiguredLimit_doesNotThrow() {
                // Arrange
                AccountInfoDto account = new AccountInfoDto(
                        "acc-005", "cust-005",
                        AccountTypeEnum.SAVINGS, null, null);

                // Act + Assert
                assertDoesNotThrow(() -> validator.validateMonthlyLimit(account, 100));
            }
        }
    }
}
