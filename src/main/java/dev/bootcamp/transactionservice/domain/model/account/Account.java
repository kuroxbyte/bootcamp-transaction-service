package dev.bootcamp.transactionservice.domain.model.account;

import dev.bootcamp.transactionservice.domain.exception.BusinessRuleException;
import dev.bootcamp.transactionservice.domain.model.movement.Movement;
import dev.bootcamp.transactionservice.domain.model.movement.MovementType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * Vista local de la cuenta, obtenida de account-service, con su monto de apertura (saldo inicial).
 *
 * @param movementPolicy      regla de movimientos según el tipo de cuenta ({@link
 *     SavingsMovementPolicy}, {@link CheckingMovementPolicy} o {@link FixedTermMovementPolicy})
 * @param openedAt            fecha de apertura de la cuenta
 * @param minimumDailyAverage monto mínimo de promedio diario que exige cada mes; solo la cuenta
 *     de ahorro VIP lo tiene, en las demás es null
 */
public record Account(
        String id,
        String customerId,
        MovementPolicy movementPolicy,
        BigDecimal openingAmount,
        LocalDate openedAt,
        BigDecimal minimumDailyAverage) {

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

    /**
     * Calcula el saldo promedio diario de la cuenta en un mes.
     *
     * <p>Se toman los días del mes en que la cuenta estuvo abierta, desde el día de apertura (o el
     * primero del mes) hasta hoy (o el último del mes). El saldo de cada día es el saldo al final
     * de ese día: monto de apertura más depósitos menos retiros hasta ese día inclusive. El
     * promedio es la suma de esos saldos entre la cantidad de días, redondeado a 2 decimales.
     *
     * @param month     mes consultado
     * @param today     fecha actual; los días posteriores no se cuentan
     * @param movements depósitos y retiros de la cuenta (pueden incluir otros meses)
     * @return el promedio del mes junto con el mínimo que exige la cuenta, si lo tiene
     * @throws BusinessRuleException si la cuenta no estuvo abierta ningún día del mes hasta hoy
     */
    public DailyAverageBalance dailyAverageBalance(
            YearMonth month,
            LocalDate today,
            List<Movement> movements) {
        LocalDate from = later(month.atDay(1), openedAt);
        LocalDate to = earlier(month.atEndOfMonth(), today);
        if (from.isAfter(to)) {
            throw new BusinessRuleException(
                    "La cuenta no estuvo abierta ningún día del mes consultado");
        }
        BigDecimal total = BigDecimal.ZERO;
        int days = 0;
        for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
            total = total.add(balance(movementsBalanceUntil(day, movements)));
            days++;
        }
        BigDecimal average = total.divide(BigDecimal.valueOf(days), 2, RoundingMode.HALF_UP);
        return new DailyAverageBalance(month, days, average, minimumDailyAverage);
    }

    private static BigDecimal movementsBalanceUntil(LocalDate day, List<Movement> movements) {
        return movements.stream()
                .filter(movement -> !movement.date().isAfter(day))
                .map(movement -> movement.type() == MovementType.DEPOSIT
                        ? movement.amount()
                        : movement.amount().negate())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static LocalDate later(LocalDate first, LocalDate second) {
        return first.isAfter(second) ? first : second;
    }

    private static LocalDate earlier(LocalDate first, LocalDate second) {
        return first.isBefore(second) ? first : second;
    }

    private void validateSufficientFunds(Movement movement, BigDecimal balance) {
        if (movement.type() == MovementType.WITHDRAWAL
                && movement.amount().compareTo(balance) > 0) {
            throw new BusinessRuleException("Saldo insuficiente para el retiro");
        }
    }
}
