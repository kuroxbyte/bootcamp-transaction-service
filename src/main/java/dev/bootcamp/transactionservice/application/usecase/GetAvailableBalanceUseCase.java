package dev.bootcamp.transactionservice.application.usecase;

import dev.bootcamp.transactionservice.domain.exception.BusinessRuleException;
import java.math.BigDecimal;

/**
 * Caso de uso para que un cliente consulte los saldos disponibles de sus productos.
 *
 * <p>Reglas:
 * <ul>
 *   <li>El producto debe existir y pertenecer al cliente.</li>
 *   <li>Cuenta bancaria: el saldo disponible es la suma de sus depósitos menos la suma
 *       de sus retiros.</li>
 *   <li>Tarjeta de crédito: el saldo disponible es el límite de crédito menos la deuda
 *       (consumos menos pagos a la tarjeta).</li>
 * </ul>
 */
public interface GetAvailableBalanceUseCase {

    /**
     * Consulta el saldo disponible de una cuenta bancaria del cliente.
     *
     * @param customerId id del cliente que consulta
     * @param accountId  id de la cuenta en account-service
     * @return saldo disponible de la cuenta
     * @throws BusinessRuleException si la cuenta no existe o no pertenece al cliente
     */
    BigDecimal getAccountBalance(Long customerId, Long accountId);

    /**
     * Consulta el crédito disponible de una tarjeta de crédito del cliente.
     *
     * @param customerId   id del cliente que consulta
     * @param creditCardId id de la tarjeta en credit-service
     * @return crédito disponible de la tarjeta
     * @throws BusinessRuleException si la tarjeta no existe o no pertenece al cliente
     */
    BigDecimal getCreditCardAvailableCredit(Long customerId, Long creditCardId);
}
