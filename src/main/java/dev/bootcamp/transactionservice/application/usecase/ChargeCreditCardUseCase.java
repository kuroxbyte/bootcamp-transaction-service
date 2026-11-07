package dev.bootcamp.transactionservice.application.usecase;

import dev.bootcamp.transactionservice.domain.exception.BusinessRuleException;
import dev.bootcamp.transactionservice.domain.model.charge.Charge;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Caso de uso para que un cliente cargue consumos a sus tarjetas de crédito.
 *
 * <p>Reglas:
 * <ul>
 *   <li>La tarjeta de crédito debe existir en credit-service y pertenecer al cliente.</li>
 *   <li>El monto del consumo debe ser mayor a 0.</li>
 *   <li>El consumo no puede superar el crédito disponible: límite de crédito
 *       menos la deuda actual (consumos menos pagos).</li>
 * </ul>
 */
public interface ChargeCreditCardUseCase {

    /**
     * Carga un consumo a una tarjeta de crédito del cliente.
     *
     * @param customerId   id del cliente que hace el consumo
     * @param creditCardId id de la tarjeta de crédito en credit-service
     * @param amount       monto del consumo
     * @param date         fecha del consumo
     * @return el consumo guardado
     * @throws BusinessRuleException si el monto no es válido, la tarjeta no existe, no pertenece
     *                               al cliente o el consumo supera el crédito disponible
     */
    Charge charge(String customerId, String creditCardId, BigDecimal amount, LocalDate date);
}
