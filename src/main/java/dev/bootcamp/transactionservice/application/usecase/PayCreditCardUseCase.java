package dev.bootcamp.transactionservice.application.usecase;

import dev.bootcamp.transactionservice.domain.exception.BusinessRuleException;
import dev.bootcamp.transactionservice.domain.model.payment.Payment;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Caso de uso para que un cliente haga pagos de sus tarjetas de crédito.
 *
 * <p>Reglas:
 * <ul>
 *   <li>La tarjeta debe existir en credit-service y pertenecer al cliente.</li>
 *   <li>El monto del pago debe ser mayor a 0.</li>
 *   <li>El pago reduce la deuda de la tarjeta y libera crédito disponible para consumos.</li>
 * </ul>
 */
public interface PayCreditCardUseCase {

    /**
     * Registra un pago a una tarjeta de crédito del cliente.
     *
     * @param customerId   id del cliente que paga
     * @param creditCardId id de la tarjeta en credit-service
     * @param amount       monto a pagar
     * @param date         fecha del pago
     * @return el pago guardado
     * @throws BusinessRuleException si el monto no es válido, la tarjeta no existe
     *                               o no pertenece al cliente
     */
    Payment pay(String customerId, String creditCardId, BigDecimal amount, LocalDate date);
}
