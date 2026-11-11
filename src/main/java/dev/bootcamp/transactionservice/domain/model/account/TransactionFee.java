package dev.bootcamp.transactionservice.domain.model.account;

import java.math.BigDecimal;

/**
 * Comisión por transacción de una cuenta, obtenida de account-service: cada mes calendario, los
 * primeros depósitos y retiros no pagan comisión; cada uno de los siguientes paga un monto fijo.
 *
 * @param freeMonthlyTransactions cantidad de transacciones al mes sin comisión
 * @param amount                  comisión fija por cada transacción que supera esa cantidad
 */
public record TransactionFee(int freeMonthlyTransactions, BigDecimal amount) {

    /**
     * Comisión que paga una nueva transacción.
     *
     * @param movementsInMonth transacciones ya registradas en el mes, sin contar la nueva
     * @return 0 si la nueva transacción está dentro de las que no pagan comisión; si no, el monto
     *     fijo de la comisión
     */
    public BigDecimal commissionFor(long movementsInMonth) {
        return movementsInMonth < freeMonthlyTransactions ? BigDecimal.ZERO : amount;
    }
}
