package com.proyecto_final.ms_transaction.infrastructure.adapter.in.rest.mapper;

import com.proyecto_final.ms_transaction.application.port.in.dto.AccountBalanceDto;
import com.proyecto_final.ms_transaction.application.port.in.dto.AccountOperationDto;
import com.proyecto_final.ms_transaction.application.port.in.dto.CreditBalanceDto;
import com.proyecto_final.ms_transaction.application.port.in.dto.CreditOperationDto;
import com.proyecto_final.ms_transaction.domain.model.Movement;
import com.proyecto_final.ms_transaction.infrastructure.adapter.in.rest.dto.request.OperationRequestDto;
import com.proyecto_final.ms_transaction.infrastructure.adapter.in.rest.dto.response.AccountBalanceResponseDto;
import com.proyecto_final.ms_transaction.infrastructure.adapter.in.rest.dto.response.CreditBalanceResponseDto;
import com.proyecto_final.ms_transaction.infrastructure.adapter.in.rest.dto.response.MovementResponseDto;
import org.springframework.stereotype.Component;

/**
 * Mapeo manual entre los DTOs REST, los DTOs de application y el modelo de dominio.
 */
@Component
public class TransactionRestMapper {

    /**
     * Convierte la solicitud y el id de la URL en la operación sobre una cuenta.
     */
    public AccountOperationDto toAccountOperationDto(String accountId, OperationRequestDto request) {
        return AccountOperationDto.builder()
                .accountId(accountId)
                .amount(request.amount())
                .description(request.description())
                .build();
    }

    /**
     * Convierte la solicitud y el id de la URL en la operación sobre un crédito.
     */
    public CreditOperationDto toCreditOperationDto(String creditId, OperationRequestDto request) {
        return CreditOperationDto.builder()
                .creditId(creditId)
                .amount(request.amount())
                .description(request.description())
                .build();
    }

    /**
     * Convierte el movimiento del dominio en el DTO de respuesta.
     */
    public MovementResponseDto toResponse(Movement movement) {
        return MovementResponseDto.builder()
                .id(movement.getId())
                .productCategory(movement.getProductCategory())
                .productId(movement.getProductId())
                .type(movement.getType())
                .amount(movement.getAmount())
                .balanceAfter(movement.getBalanceAfter())
                .description(movement.getDescription())
                .createdAt(movement.getCreatedAt())
                .build();
    }

    /**
     * Convierte el saldo de cuenta de application en el DTO de respuesta.
     */
    public AccountBalanceResponseDto toResponse(AccountBalanceDto accountBalanceDto) {
        return AccountBalanceResponseDto.builder()
                .accountId(accountBalanceDto.accountId())
                .accountType(accountBalanceDto.accountType())
                .balance(accountBalanceDto.balance())
                .build();
    }

    /**
     * Convierte el saldo de crédito de application en el DTO de respuesta.
     */
    public CreditBalanceResponseDto toResponse(CreditBalanceDto creditBalanceDto) {
        return CreditBalanceResponseDto.builder()
                .creditId(creditBalanceDto.creditId())
                .creditType(creditBalanceDto.creditType())
                .outstandingDebt(creditBalanceDto.outstandingDebt())
                .availableCredit(creditBalanceDto.availableCredit())
                .build();
    }
}
