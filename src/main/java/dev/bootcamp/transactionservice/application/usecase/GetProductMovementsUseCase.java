package dev.bootcamp.transactionservice.application.usecase;

import dev.bootcamp.transactionservice.domain.exception.BusinessRuleException;
import dev.bootcamp.transactionservice.domain.model.history.ProductMovement;
import java.util.List;

/**
 * Caso de uso para consultar todos los movimientos de un producto bancario de un cliente.
 *
 * <p>Reglas:
 * <ul>
 *   <li>El producto debe existir y pertenecer al cliente.</li>
 *   <li>Cuenta bancaria: sus depósitos y retiros.</li>
 *   <li>Crédito personal o empresarial: sus pagos.</li>
 *   <li>Tarjeta de crédito: sus consumos y pagos.</li>
 *   <li>Los movimientos se devuelven ordenados por fecha, del más antiguo al más reciente;
 *       una lista vacía si el producto no tiene movimientos.</li>
 * </ul>
 */
public interface GetProductMovementsUseCase {

    /**
     * Consulta los depósitos y retiros de una cuenta bancaria del cliente.
     *
     * @param customerId id del cliente que consulta
     * @param accountId  id de la cuenta en account-service
     * @return movimientos de la cuenta ordenados por fecha
     * @throws BusinessRuleException si la cuenta no existe o no pertenece al cliente
     */
    List<ProductMovement> getAccountMovements(Long customerId, Long accountId);

    /**
     * Consulta los pagos de un crédito personal o empresarial del cliente.
     *
     * @param customerId id del cliente que consulta
     * @param creditId   id del crédito en credit-service
     * @return movimientos del crédito ordenados por fecha
     * @throws BusinessRuleException si el crédito no existe o no pertenece al cliente
     */
    List<ProductMovement> getCreditMovements(Long customerId, Long creditId);

    /**
     * Consulta los consumos y pagos de una tarjeta de crédito del cliente.
     *
     * @param customerId   id del cliente que consulta
     * @param creditCardId id de la tarjeta en credit-service
     * @return movimientos de la tarjeta ordenados por fecha
     * @throws BusinessRuleException si la tarjeta no existe o no pertenece al cliente
     */
    List<ProductMovement> getCreditCardMovements(Long customerId, Long creditCardId);
}
