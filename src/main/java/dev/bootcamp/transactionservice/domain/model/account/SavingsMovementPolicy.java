package dev.bootcamp.transactionservice.domain.model.account;

import dev.bootcamp.transactionservice.domain.exception.BusinessRuleException;
import dev.bootcamp.transactionservice.domain.model.movement.Movement;

/**
 * Cuenta de ahorro: se permite un movimiento mientras {@code movementsInMonth} sea menor que
 * {@code maxMonthlyMovements}. Por ejemplo, con un límite de 2 se aceptan el 1.º y el 2.º
 * movimiento del mes y se rechaza el 3.º.
 */
public record SavingsMovementPolicy(int maxMonthlyMovements) implements MovementPolicy {

    /**
     * @throws BusinessRuleException si ya se alcanzó el límite mensual de movimientos
     */
    @Override
    public void validate(Movement movement, long movementsInMonth) {
        if (movementsInMonth >= maxMonthlyMovements) {
            throw new BusinessRuleException("Se alcanzó el límite de " + maxMonthlyMovements + " movimientos mensuales");
        }
    }
}
