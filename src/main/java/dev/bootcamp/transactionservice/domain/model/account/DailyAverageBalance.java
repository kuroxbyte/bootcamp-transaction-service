package dev.bootcamp.transactionservice.domain.model.account;

import java.math.BigDecimal;
import java.time.YearMonth;

/**
 * Saldo promedio diario de una cuenta en un mes. Consultarlo no bloquea ni cobra nada; al terminar
 * el mes, la cuenta de ahorro VIP que no alcanzó el mínimo paga una comisión (ver
 * {@link Account#monthlyCharges}).
 *
 * @param month               mes calculado
 * @param days                días del mes en que la cuenta estuvo abierta hasta hoy
 * @param average             saldo promedio diario de esos días
 * @param minimumDailyAverage mínimo de promedio diario que exige la cuenta; null si no exige uno
 */
public record DailyAverageBalance(
        YearMonth month,
        int days,
        BigDecimal average,
        BigDecimal minimumDailyAverage) {

    /**
     * Indica si el promedio alcanza el mínimo exigido.
     *
     * @return true si lo alcanza, false si no, o null si la cuenta no exige un mínimo
     */
    public Boolean meetsMinimum() {
        return minimumDailyAverage == null ? null : average.compareTo(minimumDailyAverage) >= 0;
    }
}
