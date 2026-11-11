package dev.bootcamp.transactionservice.domain.model.movement;

import dev.bootcamp.transactionservice.domain.exception.BusinessRuleException;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Depósito o retiro registrado en una cuenta bancaria, con la comisión que pagó.
 *
 * @param commission comisión cobrada por la transacción (0 si no pagó comisión); se descuenta del
 *     saldo de la cuenta
 */
public record Movement(
        String accountId,
        MovementType type,
        BigDecimal amount,
        LocalDate date,
        BigDecimal commission) {

    /**
     * Valida que el monto sea mayor a 0 y que la comisión no sea negativa.
     */
    public Movement {
        if (amount == null || amount.signum() <= 0) {
            throw new BusinessRuleException("El monto debe ser mayor a 0");
        }
        if (commission == null || commission.signum() < 0) {
            throw new BusinessRuleException("La comisión no puede ser negativa");
        }
    }

    /**
     * Crea un movimiento todavía sin comisión.
     */
    public Movement(String accountId, MovementType type, BigDecimal amount, LocalDate date) {
        this(accountId, type, amount, date, BigDecimal.ZERO);
    }

    /**
     * Copia del movimiento con la comisión indicada.
     */
    public Movement withCommission(BigDecimal commission) {
        return new Movement(accountId, type, amount, date, commission);
    }

    /**
     * Cuánto cambia el saldo de la cuenta por este movimiento: un depósito suma su monto menos la
     * comisión; un retiro resta su monto más la comisión.
     */
    public BigDecimal balanceEffect() {
        return type == MovementType.DEPOSIT
                ? amount.subtract(commission)
                : amount.add(commission).negate();
    }
}
