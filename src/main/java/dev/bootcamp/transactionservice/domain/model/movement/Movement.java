package dev.bootcamp.transactionservice.domain.model.movement;

import dev.bootcamp.transactionservice.domain.exception.BusinessRuleException;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Depósito, retiro, transferencia o pago con tarjeta de débito registrado en una cuenta bancaria,
 * con la comisión que pagó.
 *
 * @param commission  comisión cobrada por la transacción (0 si no pagó comisión); se descuenta del
 *     saldo de la cuenta
 * @param debitCardId tarjeta de débito con la que se pagó; solo en
 *     {@link MovementType#DEBIT_CARD_PAYMENT}, en los demás es null
 */
public record Movement(
        String accountId,
        MovementType type,
        BigDecimal amount,
        LocalDate date,
        BigDecimal commission,
        String debitCardId) {

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
        this(accountId, type, amount, date, BigDecimal.ZERO, null);
    }

    /**
     * Crea un movimiento con comisión, sin tarjeta de débito.
     */
    public Movement(
            String accountId,
            MovementType type,
            BigDecimal amount,
            LocalDate date,
            BigDecimal commission) {
        this(accountId, type, amount, date, commission, null);
    }

    /**
     * Crea un pago con tarjeta de débito, todavía sin comisión.
     */
    public static Movement debitCardPayment(
            String accountId,
            String debitCardId,
            BigDecimal amount,
            LocalDate date) {
        return new Movement(
                accountId,
                MovementType.DEBIT_CARD_PAYMENT,
                amount,
                date,
                BigDecimal.ZERO,
                debitCardId);
    }

    /**
     * Copia del movimiento con la comisión indicada.
     */
    public Movement withCommission(BigDecimal commission) {
        return new Movement(accountId, type, amount, date, commission, debitCardId);
    }

    /**
     * Cuánto cambia el saldo de la cuenta por este movimiento: un depósito o transferencia
     * recibida suma su monto menos la comisión; un retiro o transferencia enviada resta su monto
     * más la comisión.
     */
    public BigDecimal balanceEffect() {
        return type.addsToBalance()
                ? amount.subtract(commission)
                : amount.add(commission).negate();
    }
}
