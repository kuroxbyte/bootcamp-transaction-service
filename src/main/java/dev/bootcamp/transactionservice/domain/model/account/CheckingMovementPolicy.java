package dev.bootcamp.transactionservice.domain.model.account;

import dev.bootcamp.transactionservice.domain.model.movement.Movement;

/**
 * Cuenta corriente: siempre se permite el movimiento, no hay límite de movimientos mensuales.
 */
public record CheckingMovementPolicy() implements MovementPolicy {

    @Override
    public void validate(Movement movement, long movementsInMonth) {
        // Sin límite de movimientos.
    }
}
