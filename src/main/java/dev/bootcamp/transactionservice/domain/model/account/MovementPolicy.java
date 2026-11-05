package dev.bootcamp.transactionservice.domain.model.account;

import dev.bootcamp.transactionservice.domain.exception.BusinessRuleException;
import dev.bootcamp.transactionservice.domain.model.movement.Movement;

/**
 * Regla de movimientos propia de un tipo de cuenta.
 *
 * <p>Cada tipo de cuenta tiene su implementación; agregar un tipo nuevo es agregar una
 * implementación, sin modificar las existentes.
 */
public interface MovementPolicy {

    /**
     * Valida si un nuevo movimiento (depósito o retiro) está permitido para este tipo de cuenta.
     *
     * <p>No modifica nada: si el movimiento es válido termina sin efecto; si no, lanza una
     * excepción. Los depósitos y los retiros cuentan por igual, y el mes se determina por la
     * fecha del movimiento, así que el conteo se reinicia al empezar cada mes calendario.
     *
     * @param movement         movimiento a validar
     * @param movementsInMonth cantidad de movimientos ya guardados en esta cuenta durante el mes
     *                         calendario de {@code movement.date()}, sin contar el nuevo
     * @throws BusinessRuleException si el movimiento no está permitido
     */
    void validate(Movement movement, long movementsInMonth);
}
