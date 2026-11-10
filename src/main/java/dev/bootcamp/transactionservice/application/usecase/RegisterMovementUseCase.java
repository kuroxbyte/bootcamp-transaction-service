package dev.bootcamp.transactionservice.application.usecase;

import dev.bootcamp.transactionservice.domain.exception.BusinessRuleException;
import dev.bootcamp.transactionservice.domain.exception.NotFoundException;
import dev.bootcamp.transactionservice.domain.model.movement.Movement;
import io.reactivex.rxjava3.core.Single;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Caso de uso para que un cliente haga depósitos y retiros en sus cuentas bancarias.
 *
 * <p>Reglas:
 *
 * <ul>
 *   <li>La cuenta debe existir en account-service y pertenecer al cliente.
 *   <li>El monto debe ser mayor a 0.
 *   <li>Un retiro no puede superar el saldo de la cuenta (monto de apertura más depósitos
 *       menos retiros).
 *   <li>Ahorro: no se puede superar el límite máximo de movimientos mensuales.
 *   <li>Cuenta corriente: sin límite de movimientos.
 *   <li>Plazo fijo: un solo movimiento al mes, en el día configurado.
 * </ul>
 *
 * <p>Es reactivo: no hace nada hasta que se suscribe al {@link Single}. Termina con error {@link
 * NotFoundException} si el cliente o el producto no existe (o no pertenece al cliente), y con error
 * {@link BusinessRuleException} si se incumple una regla de negocio; en ambos casos no se guarda
 * nada.
 */
public interface RegisterMovementUseCase {

    /**
     * Registra un depósito en una cuenta del cliente.
     *
     * @param customerId id del cliente que hace el depósito
     * @param accountId id de la cuenta en account-service
     * @param amount monto a depositar
     * @param date fecha del movimiento
     * @return el movimiento guardado; error {@link NotFoundException} si la cuenta no existe o no
     *     pertenece al cliente; {@link BusinessRuleException} si el monto no es válido o se
     *     incumple una regla de la cuenta
     */
    Single<Movement> deposit(
            String customerId,
            String accountId,
            BigDecimal amount,
            LocalDate date);

    /**
     * Registra un retiro en una cuenta del cliente.
     *
     * @param customerId id del cliente que hace el retiro
     * @param accountId id de la cuenta en account-service
     * @param amount monto a retirar
     * @param date fecha del movimiento
     * @return el movimiento guardado; error {@link NotFoundException} si la cuenta no existe o no
     *     pertenece al cliente; {@link BusinessRuleException} si el monto no es válido, supera el
     *     saldo o se incumple una regla de la cuenta
     */
    Single<Movement> withdraw(
            String customerId,
            String accountId,
            BigDecimal amount,
            LocalDate date);
}
