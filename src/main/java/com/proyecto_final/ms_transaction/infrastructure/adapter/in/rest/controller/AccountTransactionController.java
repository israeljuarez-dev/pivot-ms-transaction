package com.proyecto_final.ms_transaction.infrastructure.adapter.in.rest.controller;

import com.proyecto_final.ms_transaction.application.port.in.use_cases.DepositUseCase;
import com.proyecto_final.ms_transaction.application.port.in.use_cases.FindAccountBalanceUseCase;
import com.proyecto_final.ms_transaction.application.port.in.use_cases.FindAccountMovementsUseCase;
import com.proyecto_final.ms_transaction.application.port.in.use_cases.WithdrawUseCase;
import com.proyecto_final.ms_transaction.infrastructure.adapter.in.rest.dto.request.OperationRequestDto;
import com.proyecto_final.ms_transaction.infrastructure.adapter.in.rest.dto.response.AccountBalanceResponseDto;
import com.proyecto_final.ms_transaction.infrastructure.adapter.in.rest.dto.response.MovementResponseDto;
import com.proyecto_final.ms_transaction.infrastructure.adapter.in.rest.mapper.TransactionRestMapper;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST reactivo de las operaciones, saldos y movimientos de cuentas.
 */
@RestController
@RequestMapping("/transactions/accounts")
@RequiredArgsConstructor
public class AccountTransactionController {

    private final DepositUseCase depositUseCase;

    private final WithdrawUseCase withdrawUseCase;

    private final FindAccountBalanceUseCase findAccountBalanceUseCase;

    private final FindAccountMovementsUseCase findAccountMovementsUseCase;

    private final TransactionRestMapper transactionRestMapper;

    /**
     * Deposita dinero en una cuenta.
     */
    @PostMapping("/{accountId}/deposits")
    @ResponseStatus(HttpStatus.CREATED)
    public Single<MovementResponseDto> deposit(
            @PathVariable("accountId") String accountId,
            @Valid @RequestBody OperationRequestDto request
    ) {
        return depositUseCase.deposit(transactionRestMapper.toAccountOperationDto(accountId, request))
                .map(transactionRestMapper::toResponse);
    }

    /**
     * Retira dinero de una cuenta.
     */
    @PostMapping("/{accountId}/withdrawals")
    @ResponseStatus(HttpStatus.CREATED)
    public Single<MovementResponseDto> withdraw(
            @PathVariable("accountId") String accountId,
            @Valid @RequestBody OperationRequestDto request
    ) {
        return withdrawUseCase.withdraw(transactionRestMapper.toAccountOperationDto(accountId, request))
                .map(transactionRestMapper::toResponse);
    }

    /**
     * Consulta el saldo disponible de una cuenta.
     */
    @GetMapping("/{accountId}/balance")
    public Single<AccountBalanceResponseDto> findBalance(@PathVariable("accountId") String accountId) {
        return findAccountBalanceUseCase.findBalance(accountId)
                .map(transactionRestMapper::toResponse);
    }

    /**
     * Lista los movimientos de una cuenta, del más reciente al más antiguo.
     */
    @GetMapping("/{accountId}/movements")
    public Flowable<MovementResponseDto> findMovements(@PathVariable("accountId") String accountId) {
        return findAccountMovementsUseCase.findMovements(accountId)
                .map(transactionRestMapper::toResponse);
    }
}
