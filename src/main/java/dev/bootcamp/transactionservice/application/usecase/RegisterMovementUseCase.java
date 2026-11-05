package dev.bootcamp.transactionservice.application.usecase;

import dev.bootcamp.transactionservice.domain.exception.BusinessRuleException;
import dev.bootcamp.transactionservice.domain.model.movement.Movement;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Caso de uso para que un cliente haga depósitos y retiros en sus cuentas bancarias.
 *
 * <p>Reglas:
 * <ul>
 *   <li>La cuenta debe existir en account-service y pertenecer al cliente.</li>
 *   <li>El monto debe ser mayor a 0.</li>
 *   <li>Ahorro: no se puede superar el límite máximo de movimientos mensuales.</li>
 *   <li>Cuenta corriente: sin límite de movimientos.</li>
 *   <li>Plazo fijo: un solo movimiento al mes, en el día configurado.</li>
 * </ul>
 */
public interface RegisterMovementUseCase {

    /**
     * Registra un depósito en una cuenta del cliente.
     *
     * @param customerId id del cliente que hace el depósito
     * @param accountId  id de la cuenta en account-service
     * @param amount     monto a depositar
     * @param date       fecha del movimiento
     * @return el movimiento guardado
     * @throws BusinessRuleException si la cuenta no existe, no pertenece al cliente,
     *                               el monto no es válido o se incumple una regla de la cuenta
     */
    Movement deposit(Long customerId, Long accountId, BigDecimal amount, LocalDate date);

    /**
     * Registra un retiro en una cuenta del cliente.
     *
     * @param customerId id del cliente que hace el retiro
     * @param accountId  id de la cuenta en account-service
     * @param amount     monto a retirar
     * @param date       fecha del movimiento
     * @return el movimiento guardado
     * @throws BusinessRuleException si la cuenta no existe, no pertenece al cliente,
     *                               el monto no es válido o se incumple una regla de la cuenta
     */
    Movement withdraw(Long customerId, Long accountId, BigDecimal amount, LocalDate date);
}
