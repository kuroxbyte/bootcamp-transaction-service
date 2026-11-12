package dev.bootcamp.transactionservice.application.usecase;

import dev.bootcamp.transactionservice.domain.exception.BusinessRuleException;
import dev.bootcamp.transactionservice.domain.exception.NotFoundException;
import dev.bootcamp.transactionservice.domain.model.payment.Payment;
import io.reactivex.rxjava3.core.Single;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Caso de uso para que un cliente pague créditos personales o empresariales: los suyos o los de
 * otro cliente del banco.
 *
 * <p>Reglas:
 *
 * <ul>
 *   <li>El crédito debe existir en credit-service.
 *   <li>Pago propio: el crédito debe pertenecer al cliente.
 *   <li>Pago a un tercero: el crédito debe ser de otro cliente.
 *   <li>El monto del pago debe ser mayor a 0.
 *   <li>El pago guarda quién pagó y reduce la deuda del dueño del crédito.
 *   <li>Al registrarlo publica el evento del pago en {@code transaction-events}, con el dueño del
 *       crédito como cliente.
 * </ul>
 *
 * <p>Es reactivo: no hace nada hasta que se suscribe al {@link Single}. Si se incumple una regla
 * no se guarda nada.
 */
public interface PayCreditUseCase {

    /**
     * Registra un pago a un crédito del cliente.
     *
     * @param customerId id del cliente que paga y es dueño del crédito
     * @param creditId id del crédito en credit-service
     * @param amount monto a pagar
     * @param date fecha del pago
     * @return el pago guardado; error {@link NotFoundException} si el crédito no existe o no
     *     pertenece al cliente; {@link BusinessRuleException} si el monto no es válido
     */
    Single<Payment> pay(String customerId, String creditId, BigDecimal amount, LocalDate date);

    /**
     * Registra un pago a un crédito de otro cliente del banco.
     *
     * @param customerId id del cliente que paga
     * @param creditId id del crédito de un tercero en credit-service
     * @param amount monto a pagar
     * @param date fecha del pago
     * @return el pago guardado; error {@link NotFoundException} si el crédito no existe; {@link
     *     BusinessRuleException} si el crédito es del propio cliente o el monto no es válido
     */
    Single<Payment> payThirdParty(
            String customerId,
            String creditId,
            BigDecimal amount,
            LocalDate date);
}
