package dev.bootcamp.transactionservice.application.usecase;

import dev.bootcamp.transactionservice.domain.exception.BusinessRuleException;
import dev.bootcamp.transactionservice.domain.model.movement.Movement;
import dev.bootcamp.transactionservice.domain.model.movement.MovementType;
import java.time.LocalDate;

/**
 * Caso de uso para registrar un movimiento (depósito o retiro) en una cuenta bancaria.
 *
 * <p>Reglas:
 * <ul>
 *   <li>La cuenta debe existir en account-service.</li>
 *   <li>Ahorro: no se puede superar el límite máximo de movimientos mensuales.</li>
 *   <li>Cuenta corriente: sin límite de movimientos.</li>
 *   <li>Plazo fijo: un solo movimiento al mes, en el día configurado.</li>
 * </ul>
 */
public interface RegisterMovementUseCase {

    /**
     * Valida y guarda un movimiento.
     *
     * @param accountId id de la cuenta en account-service
     * @param type      depósito o retiro
     * @param date      fecha del movimiento
     * @return el movimiento guardado
     * @throws BusinessRuleException si la cuenta no existe o el movimiento
     *                               no cumple las reglas de su tipo de cuenta
     */
    Movement execute(Long accountId, MovementType type, LocalDate date);
}
