package com.proyecto_final.ms_transaction.infrastructure.adapter.in.rest.controller;

import com.proyecto_final.ms_transaction.application.port.in.use_cases.ConsumeCreditUseCase;
import com.proyecto_final.ms_transaction.application.port.in.use_cases.FindCreditBalanceUseCase;
import com.proyecto_final.ms_transaction.application.port.in.use_cases.FindCreditMovementsUseCase;
import com.proyecto_final.ms_transaction.application.port.in.use_cases.PayCreditUseCase;
import com.proyecto_final.ms_transaction.infrastructure.adapter.in.rest.dto.request.OperationRequestDto;
import com.proyecto_final.ms_transaction.infrastructure.adapter.in.rest.dto.response.CreditBalanceResponseDto;
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
 * Controlador REST reactivo de los pagos, consumos, saldos y movimientos de créditos.
 */
@RestController
@RequestMapping("/transactions/credits")
@RequiredArgsConstructor
public class CreditTransactionController {

    private final PayCreditUseCase payCreditUseCase;

    private final ConsumeCreditUseCase consumeCreditUseCase;

    private final FindCreditBalanceUseCase findCreditBalanceUseCase;

    private final FindCreditMovementsUseCase findCreditMovementsUseCase;

    private final TransactionRestMapper transactionRestMapper;

    /**
     * Paga un producto de crédito.
     */
    @PostMapping("/{creditId}/payments")
    @ResponseStatus(HttpStatus.CREATED)
    public Single<MovementResponseDto> pay(
            @PathVariable("creditId") String creditId,
            @Valid @RequestBody OperationRequestDto request
    ) {
        return payCreditUseCase.pay(transactionRestMapper.toCreditOperationDto(creditId, request))
                .map(transactionRestMapper::toResponse);
    }

    /**
     * Carga un consumo a una tarjeta de crédito.
     */
    @PostMapping("/{creditId}/consumptions")
    @ResponseStatus(HttpStatus.CREATED)
    public Single<MovementResponseDto> consume(
            @PathVariable("creditId") String creditId,
            @Valid @RequestBody OperationRequestDto request
    ) {
        return consumeCreditUseCase.consume(transactionRestMapper.toCreditOperationDto(creditId, request))
                .map(transactionRestMapper::toResponse);
    }

    /**
     * Consulta la deuda pendiente y el crédito disponible de un producto de crédito.
     */
    @GetMapping("/{creditId}/balance")
    public Single<CreditBalanceResponseDto> findBalance(@PathVariable("creditId") String creditId) {
        return findCreditBalanceUseCase.findBalance(creditId)
                .map(transactionRestMapper::toResponse);
    }

    /**
     * Lista los movimientos de un crédito, del más reciente al más antiguo.
     */
    @GetMapping("/{creditId}/movements")
    public Flowable<MovementResponseDto> findMovements(@PathVariable("creditId") String creditId) {
        return findCreditMovementsUseCase.findMovements(creditId)
                .map(transactionRestMapper::toResponse);
    }
}
