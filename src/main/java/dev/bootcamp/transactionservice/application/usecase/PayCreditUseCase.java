package dev.bootcamp.transactionservice.application.usecase;

import dev.bootcamp.transactionservice.domain.exception.BusinessRuleException;
import dev.bootcamp.transactionservice.domain.exception.NotFoundException;
import dev.bootcamp.transactionservice.domain.model.payment.Payment;
import io.reactivex.rxjava3.core.Single;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Caso de uso para que un cliente haga pagos de sus créditos personales o empresariales.
 *
 * <p>Reglas:
 *
 * <ul>
 *   <li>El crédito debe existir en credit-service y pertenecer al cliente.
 *   <li>El monto del pago debe ser mayor a 0.
 *   <li>Al registrarlo publica el evento del pago en {@code transaction-events}.
 * </ul>
 *
 * <p>Es reactivo: no hace nada hasta que se suscribe al {@link Single}. Termina con error {@link
 * NotFoundException} si el cliente o el producto no existe (o no pertenece al cliente), y con error
 * {@link BusinessRuleException} si se incumple una regla de negocio; en ambos casos no se guarda
 * nada.
 */
public interface PayCreditUseCase {

    /**
     * Registra un pago a un crédito del cliente.
     *
     * @param customerId id del cliente que paga
     * @param creditId id del crédito en credit-service
     * @param amount monto a pagar
     * @param date fecha del pago
     * @return el pago guardado; error {@link NotFoundException} si el crédito no existe o no
     *     pertenece al cliente; {@link BusinessRuleException} si el monto no es válido
     */
    Single<Payment> pay(String customerId, String creditId, BigDecimal amount, LocalDate date);
}
