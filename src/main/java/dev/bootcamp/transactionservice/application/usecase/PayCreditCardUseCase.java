package dev.bootcamp.transactionservice.application.usecase;

import dev.bootcamp.transactionservice.domain.exception.BusinessRuleException;
import dev.bootcamp.transactionservice.domain.exception.NotFoundException;
import dev.bootcamp.transactionservice.domain.model.payment.Payment;
import io.reactivex.rxjava3.core.Single;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Caso de uso para que un cliente pague tarjetas de crédito: las suyas o las de otro cliente del
 * banco.
 *
 * <p>Reglas:
 *
 * <ul>
 *   <li>La tarjeta debe existir en credit-service.
 *   <li>Pago propio: la tarjeta debe pertenecer al cliente.
 *   <li>Pago a un tercero: la tarjeta debe ser de otro cliente.
 *   <li>El monto del pago debe ser mayor a 0.
 *   <li>El pago guarda quién pagó, reduce la deuda de la tarjeta y libera crédito disponible para
 *       consumos de su dueño.
 *   <li>Al registrarlo publica el evento del pago en {@code transaction-events}, con el dueño de
 *       la tarjeta como cliente.
 * </ul>
 *
 * <p>Es reactivo: no hace nada hasta que se suscribe al {@link Single}. Si se incumple una regla
 * no se guarda nada.
 */
public interface PayCreditCardUseCase {

    /**
     * Registra un pago a una tarjeta del cliente.
     *
     * @param customerId id del cliente que paga y es dueño de la tarjeta
     * @param creditCardId id de la tarjeta en credit-service
     * @param amount monto a pagar
     * @param date fecha del pago
     * @return el pago guardado; error {@link NotFoundException} si la tarjeta no existe o no
     *     pertenece al cliente; {@link BusinessRuleException} si el monto no es válido
     */
    Single<Payment> pay(String customerId, String creditCardId, BigDecimal amount, LocalDate date);

    /**
     * Registra un pago a una tarjeta de otro cliente del banco.
     *
     * @param customerId id del cliente que paga
     * @param creditCardId id de la tarjeta de un tercero en credit-service
     * @param amount monto a pagar
     * @param date fecha del pago
     * @return el pago guardado; error {@link NotFoundException} si la tarjeta no existe; {@link
     *     BusinessRuleException} si la tarjeta es del propio cliente o el monto no es válido
     */
    Single<Payment> payThirdParty(
            String customerId,
            String creditCardId,
            BigDecimal amount,
            LocalDate date);
}
