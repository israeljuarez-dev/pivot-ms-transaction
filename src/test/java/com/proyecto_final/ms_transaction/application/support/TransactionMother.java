package com.proyecto_final.ms_transaction.application.support;

import com.proyecto_final.ms_transaction.application.port.in.dto.AccountOperationDto;
import com.proyecto_final.ms_transaction.application.port.in.dto.CreditOperationDto;
import com.proyecto_final.ms_transaction.application.port.out.client.account.dto.AccountInfoDto;
import com.proyecto_final.ms_transaction.application.port.out.client.credit.dto.CreditInfoDto;
import com.proyecto_final.ms_transaction.domain.model.AccountTypeEnum;
import com.proyecto_final.ms_transaction.domain.model.CreditTypeEnum;
import com.proyecto_final.ms_transaction.domain.model.Movement;
import com.proyecto_final.ms_transaction.domain.model.MovementTypeEnum;
import com.proyecto_final.ms_transaction.domain.model.ProductBalance;
import com.proyecto_final.ms_transaction.domain.model.ProductCategoryEnum;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Object Mother: crea instancias validas y nuevas de los modelos y DTOs de ms-transaction.
 * Cada metodo devuelve una instancia nueva; nunca se comparte una misma referencia entre tests.
 */
public final class TransactionMother {

    public static final Instant FIXED_INSTANT = Instant.parse("2026-01-10T10:00:00Z");
    public static final String ACCOUNT_ID = "acc-001";
    public static final String CREDIT_ID = "cred-001";

    private TransactionMother() {
    }

    public static AccountInfoDto savingsAccount() {
        return new AccountInfoDto(ACCOUNT_ID, "cust-001", AccountTypeEnum.SAVINGS, 10, null);
    }

    public static AccountInfoDto checkingAccount() {
        return new AccountInfoDto("acc-002", "cust-002", AccountTypeEnum.CHECKING, null, null);
    }

    public static AccountInfoDto fixedTermAccount(int movementDayOfMonth) {
        return new AccountInfoDto("acc-003", "cust-003", AccountTypeEnum.FIXED_TERM, null, movementDayOfMonth);
    }

    public static CreditInfoDto creditCard() {
        return new CreditInfoDto(CREDIT_ID, "cust-001", CreditTypeEnum.CREDIT_CARD, null,
                BigDecimal.valueOf(1000));
    }

    public static CreditInfoDto loan() {
        return new CreditInfoDto("cred-002", "cust-002", CreditTypeEnum.LOAN, BigDecimal.valueOf(5000), null);
    }

    public static ProductBalance accountBalance(BigDecimal amount) {
        return ProductBalance.builder()
                .id(ProductBalance.buildId(ProductCategoryEnum.ACCOUNT, ACCOUNT_ID))
                .productCategory(ProductCategoryEnum.ACCOUNT)
                .productId(ACCOUNT_ID)
                .amount(amount)
                .version(1L)
                .updatedAt(FIXED_INSTANT)
                .build();
    }

    public static ProductBalance creditBalance(BigDecimal amount) {
        return ProductBalance.builder()
                .id(ProductBalance.buildId(ProductCategoryEnum.CREDIT, CREDIT_ID))
                .productCategory(ProductCategoryEnum.CREDIT)
                .productId(CREDIT_ID)
                .amount(amount)
                .version(1L)
                .updatedAt(FIXED_INSTANT)
                .build();
    }

    public static Movement movement(ProductCategoryEnum category, String productId, MovementTypeEnum type) {
        return Movement.builder()
                .id("mov-001")
                .productCategory(category)
                .productId(productId)
                .type(type)
                .amount(BigDecimal.valueOf(100))
                .balanceAfter(BigDecimal.valueOf(500))
                .description("test movement")
                .createdAt(FIXED_INSTANT)
                .build();
    }

    public static AccountOperationDto accountOperationDto(String accountId, BigDecimal amount) {
        return AccountOperationDto.builder()
                .accountId(accountId)
                .amount(amount)
                .description("operation")
                .build();
    }

    public static CreditOperationDto creditOperationDto(String creditId, BigDecimal amount) {
        return CreditOperationDto.builder()
                .creditId(creditId)
                .amount(amount)
                .description("operation")
                .build();
    }
}
