package com.proyecto_final.ms_transaction.domain.model;

/**
 * Tipos de cuenta bancaria. Cada tipo define cuándo y cuántos movimientos admite.
 */
public enum AccountTypeEnum {

    SAVINGS,
    CHECKING,
    FIXED_TERM;

    // La cuenta a plazo fijo admite un solo movimiento (depósito o retiro) por mes
    private static final long FIXED_TERM_MAX_MONTHLY_MOVEMENTS = 1;

    /**
     * Indica si hoy es un día permitido para mover dinero en este tipo de cuenta.
     * Solo el plazo fijo restringe el día: debe coincidir con el día configurado.
     *
     * @param movementDayOfMonth día del mes configurado en la cuenta
     * @param dayOfMonth         día del mes actual
     * @return true si se puede operar hoy
     */
    public boolean isMovementDayAllowed(Integer movementDayOfMonth, int dayOfMonth) {
        return switch (this) {
            case SAVINGS, CHECKING -> true;
            case FIXED_TERM -> movementDayOfMonth != null && movementDayOfMonth == dayOfMonth;
        };
    }

    /**
     * Indica si la cuenta ya alcanzó su máximo de movimientos del mes.
     *
     * @param monthlyMovementLimit límite mensual configurado (ahorro)
     * @param movementsThisMonth   movimientos ya registrados en el mes actual
     * @return true si no admite otro movimiento este mes
     */
    public boolean isMonthlyLimitReached(Integer monthlyMovementLimit, long movementsThisMonth) {
        return switch (this) {
            case SAVINGS -> monthlyMovementLimit != null && movementsThisMonth >= monthlyMovementLimit;
            case CHECKING -> false;
            case FIXED_TERM -> movementsThisMonth >= FIXED_TERM_MAX_MONTHLY_MOVEMENTS;
        };
    }
}
