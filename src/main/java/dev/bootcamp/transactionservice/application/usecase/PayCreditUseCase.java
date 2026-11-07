package dev.bootcamp.transactionservice.application.usecase;

import dev.bootcamp.transactionservice.domain.exception.BusinessRuleException;
import dev.bootcamp.transactionservice.domain.model.payment.Payment;
import io.reactivex.rxjava3.core.Single;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Caso de uso para que un cliente haga pagos de sus créditos personales o empresariales.
 *
 * <p>Reglas:
 * <ul>
 *   <li>El crédito debe existir en credit-service y pertenecer al cliente.</li>
 *   <li>El monto del pago debe ser mayor a 0.</li>
 * </ul>
 *
 * <p>Es reactivo: no hace nada hasta que se suscribe al {@link Single}. Si se incumple una
 * regla, el {@link Single} termina con error {@link BusinessRuleException}.
 */
public interface PayCreditUseCase {

    /**
     * Registra un pago a un crédito del cliente.
     *
     * @param customerId id del cliente que paga
     * @param creditId   id del crédito en credit-service
     * @param amount     monto a pagar
     * @param date       fecha del pago
     * @return el pago guardado; error {@link BusinessRuleException} si el monto no es válido, el
     *         crédito no existe o no pertenece al cliente
     */
    Single<Payment> pay(String customerId, String creditId, BigDecimal amount, LocalDate date);
}
