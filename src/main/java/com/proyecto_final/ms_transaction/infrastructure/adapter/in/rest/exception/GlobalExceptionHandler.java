package com.proyecto_final.ms_transaction.infrastructure.adapter.in.rest.exception;

// MODIFICADO: se reemplazó el import con comodín (domain.exception.*) por imports explícitos
import com.proyecto_final.ms_transaction.domain.exception.AccountNotFoundException;
import com.proyecto_final.ms_transaction.domain.exception.ConcurrentBalanceUpdateException;
import com.proyecto_final.ms_transaction.domain.exception.ConsumptionNotAllowedException;
import com.proyecto_final.ms_transaction.domain.exception.CreditLimitExceededException;
import com.proyecto_final.ms_transaction.domain.exception.CreditNotFoundException;
import com.proyecto_final.ms_transaction.domain.exception.InsufficientFundsException;
import com.proyecto_final.ms_transaction.domain.exception.MonthlyMovementLimitExceededException;
import com.proyecto_final.ms_transaction.domain.exception.MovementDayNotAllowedException;
import com.proyecto_final.ms_transaction.domain.exception.PaymentExceedsDebtException;
import com.proyecto_final.ms_transaction.domain.exception.ProductServiceUnavailableException;
import com.proyecto_final.ms_transaction.infrastructure.adapter.in.rest.dto.response.ErrorResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebInputException;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * Traduce las excepciones del dominio y de validación a respuestas HTTP con formato uniforme.
 */
@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final Clock clock;

    /**
     * Cuenta o crédito inexistente: 404.
     */
    @ExceptionHandler({
            AccountNotFoundException.class,
            CreditNotFoundException.class
    })
    public ResponseEntity<ErrorResponseDto> handleProductNotFound(RuntimeException exception) {
        return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage(), List.of());
    }

    /**
     * Saldo modificado por otra operación simultánea: 409, el cliente debe reintentar.
     */
    @ExceptionHandler(ConcurrentBalanceUpdateException.class)
    public ResponseEntity<ErrorResponseDto> handleConcurrentUpdate(ConcurrentBalanceUpdateException exception) {
        return buildResponse(HttpStatus.CONFLICT, exception.getMessage(), List.of());
    }

    /**
     * Reglas de negocio incumplidas (fondos, límites, día no permitido, pago sobre la deuda,
     * consumo no permitido): 422. Un solo handler evita repetir el mismo código.
     */
    @ExceptionHandler({
            InsufficientFundsException.class,
            PaymentExceedsDebtException.class,
            CreditLimitExceededException.class,
            ConsumptionNotAllowedException.class,
            MonthlyMovementLimitExceededException.class,
            MovementDayNotAllowedException.class
    })
    public ResponseEntity<ErrorResponseDto> handleBusinessRuleViolation(RuntimeException exception) {
        return buildResponse(HttpStatus.UNPROCESSABLE_CONTENT, exception.getMessage(), List.of());
    }

    /**
     * ms-account o ms-credit no disponibles: 503.
     */
    @ExceptionHandler(ProductServiceUnavailableException.class)
    public ResponseEntity<ErrorResponseDto> handleServiceUnavailable(ProductServiceUnavailableException exception) {
        return buildResponse(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage(), List.of());
    }

    /**
     * Fallo de las validaciones del DTO (@Valid): 400 con el detalle de cada campo.
     */
    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ErrorResponseDto> handleValidation(WebExchangeBindException exception) {
        List<String> details = exception.getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .toList();
        log.warn("Request validation failed: {}", details);
        return buildResponse(HttpStatus.BAD_REQUEST, "Validation failed", details);
    }

    /**
     * Cuerpo ilegible (JSON mal formado o valor de enum inexistente): 400.
     */
    @ExceptionHandler(ServerWebInputException.class)
    public ResponseEntity<ErrorResponseDto> handleUnreadableBody(ServerWebInputException exception) {
        log.warn("Unreadable request body: {}", exception.getReason());
        return buildResponse(HttpStatus.BAD_REQUEST, "Malformed request body", List.of());
    }

    /**
     * Errores HTTP propios del framework (ruta inexistente, método no permitido, etc.):
     * se respeta el código que trae la excepción.
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponseDto> handleResponseStatus(ResponseStatusException exception) {
        HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
        log.warn("Request rejected with status {}: {}", status.value(), exception.getReason());
        return buildResponse(status, exception.getReason(), List.of());
    }

    /**
     * Cualquier otro error no contemplado: 500 sin exponer detalles internos.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleUnexpected(Exception exception) {
        log.error("Unexpected error", exception);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", List.of());
    }

    /**
     * Construye la respuesta de error uniforme (evita repetir el armado en cada handler).
     */
    private ResponseEntity<ErrorResponseDto> buildResponse(
            HttpStatus status, String message, List<String> details) {
        ErrorResponseDto body = new ErrorResponseDto(
                Instant.now(clock),
                status.value(),
                status.getReasonPhrase(),
                message, details
        );
        return ResponseEntity.status(status).body(body);
    }
}