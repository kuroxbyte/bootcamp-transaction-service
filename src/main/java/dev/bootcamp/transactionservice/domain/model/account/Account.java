package dev.bootcamp.transactionservice.domain.model.account;

import dev.bootcamp.transactionservice.domain.exception.BusinessRuleException;
import dev.bootcamp.transactionservice.domain.model.movement.Movement;
import dev.bootcamp.transactionservice.domain.model.movement.MovementType;

import java.math.BigDecimal;

/**
 * Vista local de la cuenta, obtenida de account-service, con su monto de apertura (saldo inicial).
 *
 * @param movementPolicy regla de movimientos según el tipo de cuenta ({@link
 *     SavingsMovementPolicy}, {@link CheckingMovementPolicy} o {@link FixedTermMovementPolicy})
 */
public record Account(
        String id,
        String customerId,
        MovementPolicy movementPolicy,
        BigDecimal openingAmount) {

    /**
     * Saldo de la cuenta: monto de apertura más depósitos menos retiros.
     *
     * @param movementsBalance suma de depósitos menos suma de retiros
     * @return saldo actual
     */
    public BigDecimal balance(BigDecimal movementsBalance) {
        return openingAmount.add(movementsBalance);
    }

    /**
     * Indica si la cuenta pertenece al cliente.
     */
    public boolean belongsTo(String customerId) {
        return this.customerId.equals(customerId);
    }

    /**
     * Valida si un nuevo movimiento (depósito o retiro) está permitido en esta cuenta.
     *
     * <p>No modifica nada: si el movimiento es válido el método termina sin efecto y quien lo
     * invoca debe guardarlo; si no, lanza una excepción y el movimiento no debe guardarse.
     *
     * <p>Aplica dos validaciones en este orden:
     *
     * <ol>
     *   <li>La regla del tipo de cuenta, delegada en {@link #movementPolicy()} (límite mensual del
     *       ahorro, día único del plazo fijo; la corriente no tiene límite).
     *   <li>Fondos suficientes: un retiro se rechaza si su monto es mayor que {@code balance}. Un
     *       retiro por el saldo exacto sí se permite y deja la cuenta en 0. Los depósitos no
     *       dependen del saldo.
     * </ol>
     *
     * @param movement movimiento a validar
     * @param movementsInMonth cantidad de movimientos ya guardados en esta cuenta durante el mes
     *     calendario de {@code movement.date()}, sin contar el nuevo
     * @param balance saldo actual de la cuenta antes del movimiento (ver {@link #balance})
     * @throws BusinessRuleException si la regla del tipo de cuenta no lo permite o si un retiro
     *     supera el saldo disponible
     */
    public void validateMovement(Movement movement, long movementsInMonth, BigDecimal balance) {
        movementPolicy.validate(movement, movementsInMonth);
        validateSufficientFunds(movement, balance);
    }

    private void validateSufficientFunds(Movement movement, BigDecimal balance) {
        if (movement.type() == MovementType.WITHDRAWAL
                && movement.amount().compareTo(balance) > 0) {
            throw new BusinessRuleException("Saldo insuficiente para el retiro");
        }
    }
}
