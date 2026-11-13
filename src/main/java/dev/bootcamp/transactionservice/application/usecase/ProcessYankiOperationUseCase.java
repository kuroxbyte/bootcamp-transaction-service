package dev.bootcamp.transactionservice.application.usecase;

import io.reactivex.rxjava3.core.Completable;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Caso de uso para aplicar un cargo o abono que yanki-service pide sobre la cuenta principal de la
 * tarjeta de débito de un monedero, y responderle.
 *
 * <ul>
 *   <li>Cargo: movimiento {@code YANKI_PAYMENT_SENT}, con las reglas y la comisión de un retiro.
 *   <li>Abono: movimiento {@code YANKI_PAYMENT_RECEIVED}, con las reglas y la comisión de un
 *       depósito.
 *   <li>Devolución de un cargo ({@code reversal}): movimiento {@code YANKI_PAYMENT_REVERSAL}, sin
 *       reglas ni comisión y sin contar como transacción del mes.
 * </ul>
 *
 * <p>Si la cuenta no existe o se incumple una regla, la operación se rechaza con el motivo. El
 * resultado se guarda por id de operación: si el pedido llega otra vez se responde lo mismo sin
 * registrar otro movimiento. Si falla otra cosa (account-service no disponible), el error se
 * propaga para que Kafka vuelva a entregar el pedido. Es reactivo: no hace nada hasta que se
 * suscribe al {@link Completable}.
 */
public interface ProcessYankiOperationUseCase {

    /**
     * Aplica la operación y publica su resultado.
     *
     * @param operationId id de la operación en yanki-service
     * @param debit       true para un cargo; false para un abono
     * @param accountId   cuenta bancaria
     * @param amount      monto
     * @param date        fecha del pago
     * @param reversal    true si el abono devuelve un cargo anterior
     */
    Completable execute(
            String operationId,
            boolean debit,
            String accountId,
            BigDecimal amount,
            LocalDate date,
            boolean reversal);
}
