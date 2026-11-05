package dev.bootcamp.transactionservice.domain.model.account;

import dev.bootcamp.transactionservice.domain.exception.BusinessRuleException;
import dev.bootcamp.transactionservice.domain.model.movement.Movement;

/**
 * Cuenta a plazo fijo: se permite un único depósito o retiro al mes, solo si la fecha del
 * movimiento cae en {@code movementDayOfMonth} y aún no hay movimientos en ese mes.
 */
public record FixedTermMovementPolicy(int movementDayOfMonth) implements MovementPolicy {

    /**
     * @throws BusinessRuleException si el movimiento no cae en el día configurado
     *                               o si ya existe un movimiento en ese mes
     */
    @Override
    public void validate(Movement movement, long movementsInMonth) {
        if (movement.date().getDayOfMonth() != movementDayOfMonth) {
            throw new BusinessRuleException("Solo se permiten movimientos el día " + movementDayOfMonth + " del mes");
        }
        if (movementsInMonth > 0) {
            throw new BusinessRuleException("Solo se permite un movimiento al mes");
        }
    }
}
