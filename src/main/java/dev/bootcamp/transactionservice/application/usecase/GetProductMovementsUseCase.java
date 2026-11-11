package dev.bootcamp.transactionservice.application.usecase;

import dev.bootcamp.transactionservice.domain.exception.NotFoundException;
import dev.bootcamp.transactionservice.domain.model.history.ProductMovement;
import io.reactivex.rxjava3.core.Flowable;

/**
 * Caso de uso para consultar todos los movimientos de un producto bancario de un cliente.
 *
 * <p>Reglas:
 *
 * <ul>
 *   <li>El producto debe existir y pertenecer al cliente.
 *   <li>Cuenta bancaria: sus depósitos y retiros.
 *   <li>Crédito personal o empresarial: sus pagos.
 *   <li>Tarjeta de crédito: sus consumos y pagos.
 *   <li>Los movimientos se devuelven ordenados por fecha, del más antiguo al más reciente; una
 *       lista vacía si el producto no tiene movimientos.
 * </ul>
 *
 * <p>Es reactivo: no hace nada hasta que se suscribe al {@link Flowable}. Termina con error {@link
 * NotFoundException} si el producto no existe o no pertenece al cliente.
 */
public interface GetProductMovementsUseCase {

    /**
     * Cantidad de movimientos del reporte de últimos movimientos de una tarjeta.
     */
    int LAST_MOVEMENTS = 10;

    /**
     * Consulta los depósitos y retiros de una cuenta bancaria del cliente.
     *
     * @param customerId id del cliente que consulta
     * @param accountId id de la cuenta en account-service
     * @return movimientos de la cuenta ordenados por fecha; error {@link NotFoundException} si la
     *     cuenta no existe o no pertenece al cliente
     */
    Flowable<ProductMovement> getAccountMovements(String customerId, String accountId);

    /**
     * Consulta los pagos de un crédito personal o empresarial del cliente.
     *
     * @param customerId id del cliente que consulta
     * @param creditId id del crédito en credit-service
     * @return movimientos del crédito ordenados por fecha; error {@link NotFoundException} si el
     *     crédito no existe o no pertenece al cliente
     */
    Flowable<ProductMovement> getCreditMovements(String customerId, String creditId);

    /**
     * Consulta los consumos y pagos de una tarjeta de crédito del cliente.
     *
     * @param customerId id del cliente que consulta
     * @param creditCardId id de la tarjeta en credit-service
     * @return movimientos de la tarjeta ordenados por fecha; error {@link NotFoundException} si la
     *     tarjeta no existe o no pertenece al cliente
     */
    Flowable<ProductMovement> getCreditCardMovements(String customerId, String creditCardId);

    /**
     * Reporte con los últimos {@value #LAST_MOVEMENTS} movimientos (consumos y pagos) de una
     * tarjeta de crédito del cliente, del más reciente al más antiguo.
     *
     * <p>Los movimientos tienen fecha sin hora: entre movimientos del mismo día no se garantiza el
     * orden.
     *
     * <p>TODO: incluir la tarjeta de débito en este reporte cuando exista el producto.
     *
     * @param customerId   id del cliente que consulta
     * @param creditCardId id de la tarjeta en credit-service
     * @return hasta {@value #LAST_MOVEMENTS} movimientos; error {@link NotFoundException} si la
     *     tarjeta no existe o no pertenece al cliente
     */
    Flowable<ProductMovement> getCreditCardLastMovements(String customerId, String creditCardId);
}
