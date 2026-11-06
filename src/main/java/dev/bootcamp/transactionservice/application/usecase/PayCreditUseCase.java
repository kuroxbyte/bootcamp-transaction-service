package dev.bootcamp.transactionservice.application.usecase;

import dev.bootcamp.transactionservice.domain.exception.BusinessRuleException;
import dev.bootcamp.transactionservice.domain.model.payment.Payment;
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
 */
public interface PayCreditUseCase {

    /**
     * Registra un pago a un crédito del cliente.
     *
     * @param customerId id del cliente que paga
     * @param creditId   id del crédito en credit-service
     * @param amount     monto a pagar
     * @param date       fecha del pago
     * @return el pago guardado
     * @throws BusinessRuleException si el monto no es válido, el crédito no existe
     *                               o no pertenece al cliente
     */
    Payment pay(Long customerId, Long creditId, BigDecimal amount, LocalDate date);
}
