package dev.bootcamp.transactionservice.domain.model.account;

import dev.bootcamp.transactionservice.domain.exception.BusinessRuleException;
import dev.bootcamp.transactionservice.domain.model.movement.Movement;

/**
 * Vista local de la cuenta, obtenida de account-service.
 *
 * @param maxMonthlyMovements solo para SAVINGS
 * @param movementDayOfMonth  solo para FIXED_TERM
 */
public record Account(Long id, Long customerId, AccountType type, Integer maxMonthlyMovements, Integer movementDayOfMonth) {

    /**
     * Indica si la cuenta pertenece al cliente.
     */
    public boolean belongsTo(Long customerId) {
        return this.customerId.equals(customerId);
    }

    /**
     * Valida un nuevo movimiento según las reglas del tipo de cuenta.
     *
     * @param movement         movimiento a registrar
     * @param movementsInMonth movimientos ya registrados en el mes del movimiento
     * @throws BusinessRuleException si el movimiento no está permitido
     */
    public void validateMovement(Movement movement, long movementsInMonth) {
        switch (type) {
            case SAVINGS -> {
                if (movementsInMonth >= maxMonthlyMovements) {
                    throw new BusinessRuleException("Se alcanzó el límite de " + maxMonthlyMovements + " movimientos mensuales");
                }
            }
            case CHECKING -> {
                // Sin límite de movimientos.
            }
            case FIXED_TERM -> {
                if (movement.date().getDayOfMonth() != movementDayOfMonth) {
                    throw new BusinessRuleException("Solo se permiten movimientos el día " + movementDayOfMonth + " del mes");
                }
                if (movementsInMonth > 0) {
                    throw new BusinessRuleException("Solo se permite un movimiento al mes");
                }
            }
        }
    }
}
