package dev.bootcamp.transactionservice.domain.model.account;

import dev.bootcamp.transactionservice.domain.exception.BusinessRuleException;
import dev.bootcamp.transactionservice.domain.model.movement.Movement;

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
 * @param transactionFee      transacciones al mes sin comisión y comisión por cada transacción
 *     adicional
 */
public record Account(
        String id,
        String customerId,
        MovementPolicy movementPolicy,
        BigDecimal openingAmount,
        LocalDate openedAt,
        BigDecimal minimumDailyAverage,
        TransactionFee transactionFee) {

    /**
     * Saldo de la cuenta: monto de apertura más depósitos menos retiros, descontadas las
     * comisiones.
     *
     * @param movementsBalance suma de depósitos menos suma de retiros y de comisiones
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
     * Valida un nuevo movimiento (depósito, retiro o transferencia) y le asigna la comisión que le
     * corresponde.
     *
     * <p>No guarda nada: si el movimiento es válido devuelve una copia con su comisión, que quien
     * lo invoca debe guardar; si no, lanza una excepción y el movimiento no debe guardarse.
     *
     * <p>Aplica en este orden:
     *
     * <ol>
     *   <li>La regla del tipo de cuenta, delegada en {@link #movementPolicy()} (límite mensual del
     *       ahorro, día único del plazo fijo; la corriente no tiene límite).
     *   <li>La comisión por transacción ({@link #transactionFee()}): 0 mientras el movimiento esté
     *       dentro de las transacciones sin comisión del mes; si no, el monto fijo de la comisión.
     *   <li>Fondos suficientes: el saldo después del movimiento y su comisión no puede quedar
     *       negativo. Un retiro debe cubrir monto más comisión (puede dejar la cuenta en 0); un
     *       depósito con comisión requiere que el saldo cubra la parte de la comisión que el
     *       depósito no alcanza a pagar.
     * </ol>
     *
     * @param movement         movimiento a validar, todavía sin comisión
     * @param movementsInMonth cantidad de movimientos ya guardados en esta cuenta durante el mes
     *     calendario de {@code movement.date()}, sin contar el nuevo
     * @param balance          saldo actual de la cuenta antes del movimiento (ver {@link #balance})
     * @return el movimiento con su comisión
     * @throws BusinessRuleException si la regla del tipo de cuenta no lo permite o si el saldo no
     *     alcanza para el movimiento y su comisión
     */
    public Movement prepareMovement(Movement movement, long movementsInMonth, BigDecimal balance) {
        movementPolicy.validate(movement, movementsInMonth);
        Movement charged =
                movement.withCommission(transactionFee.commissionFor(movementsInMonth));
        validateSufficientFunds(charged, balance);
        return charged;
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
                .map(Movement::balanceEffect)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static LocalDate later(LocalDate first, LocalDate second) {
        return first.isAfter(second) ? first : second;
    }

    private static LocalDate earlier(LocalDate first, LocalDate second) {
        return first.isBefore(second) ? first : second;
    }

    private static void validateSufficientFunds(Movement movement, BigDecimal balance) {
        if (balance.add(movement.balanceEffect()).signum() >= 0) {
            return;
        }
        throw new BusinessRuleException(
                switch (movement.type()) {
                    case WITHDRAWAL -> "Saldo insuficiente para el retiro y su comisión";
                    case TRANSFER_OUT -> "Saldo insuficiente para la transferencia y su comisión";
                    case DEPOSIT, TRANSFER_IN -> "Saldo insuficiente para la comisión del depósito";
                });
    }
}
