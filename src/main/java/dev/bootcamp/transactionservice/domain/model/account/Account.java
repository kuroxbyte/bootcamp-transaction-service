package dev.bootcamp.transactionservice.domain.model.account;

import dev.bootcamp.transactionservice.domain.exception.BusinessRuleException;
import dev.bootcamp.transactionservice.domain.model.movement.Movement;
import dev.bootcamp.transactionservice.domain.model.movement.MovementType;
import java.math.BigDecimal;

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
     * Valida si un nuevo movimiento (depósito o retiro) está permitido según el tipo de cuenta.
     *
     * <p>No modifica nada: si el movimiento es válido el método termina sin efecto y quien
     * lo invoca debe guardarlo; si no, lanza una excepción y el movimiento no debe guardarse.
     *
     * <p>Reglas por tipo de cuenta:
     * <ul>
     *   <li>{@link AccountType#SAVINGS}: se permite mientras {@code movementsInMonth} sea menor
     *       que {@code maxMonthlyMovements}. Por ejemplo, con un límite de 2 se aceptan el 1.º y
     *       el 2.º movimiento del mes y se rechaza el 3.º.</li>
     *   <li>{@link AccountType#CHECKING}: siempre se permite, no hay límite de movimientos.</li>
     *   <li>{@link AccountType#FIXED_TERM}: se permite solo si la fecha del movimiento cae en
     *       {@code movementDayOfMonth} y aún no hay movimientos en ese mes ({@code movementsInMonth == 0}).
     *       Es decir, un único depósito o retiro al mes, en el día configurado.</li>
     * </ul>
     *
     * <p>En todos los tipos de cuenta, un retiro se rechaza si su monto es mayor que
     * {@code balance}. Un retiro por el saldo exacto sí se permite y deja la cuenta en 0.
     * Los depósitos no dependen del saldo.
     *
     * <p>Los depósitos y los retiros cuentan por igual para los límites. El mes se determina por
     * la fecha del movimiento, así que el conteo se reinicia al empezar cada mes calendario.
     *
     * @param movement         movimiento a validar; se usa su fecha para la regla de plazo fijo
     * @param movementsInMonth cantidad de movimientos ya guardados en esta cuenta durante el mes
     *                         calendario de {@code movement.date()}, sin contar el nuevo
     * @param balance          saldo actual de la cuenta antes del movimiento
     * @throws BusinessRuleException si se alcanzó el límite mensual de la cuenta de ahorro, si el
     *                               movimiento de plazo fijo no cae en el día configurado, si
     *                               ya existe un movimiento de plazo fijo en ese mes o si un
     *                               retiro supera el saldo disponible
     */
    public void validateMovement(Movement movement, long movementsInMonth, BigDecimal balance) {
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
        if (movement.type() == MovementType.WITHDRAWAL && movement.amount().compareTo(balance) > 0) {
            throw new BusinessRuleException("Saldo insuficiente para el retiro");
        }
    }
}
