package dev.bootcamp.transactionservice.application.usecase;

import dev.bootcamp.transactionservice.domain.exception.BusinessRuleException;
import dev.bootcamp.transactionservice.domain.exception.NotFoundException;
import dev.bootcamp.transactionservice.domain.model.charge.Charge;
import io.reactivex.rxjava3.core.Single;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Caso de uso para que un cliente cargue consumos a sus tarjetas de crédito.
 *
 * <p>Reglas:
 *
 * <ul>
 *   <li>La tarjeta de crédito debe existir en credit-service y pertenecer al cliente.
 *   <li>El monto del consumo debe ser mayor a 0.
 *   <li>El consumo no puede superar el crédito disponible: límite de crédito menos la deuda actual
 *       (consumos menos pagos).
 * </ul>
 *
 * <p>Es reactivo: no hace nada hasta que se suscribe al {@link Single}. Termina con error {@link
 * NotFoundException} si el cliente o el producto no existe (o no pertenece al cliente), y con error
 * {@link BusinessRuleException} si se incumple una regla de negocio; en ambos casos no se guarda
 * nada.
 */
public interface ChargeCreditCardUseCase {

    /**
     * Carga un consumo a una tarjeta de crédito del cliente.
     *
     * @param customerId id del cliente que hace el consumo
     * @param creditCardId id de la tarjeta de crédito en credit-service
     * @param amount monto del consumo
     * @param date fecha del consumo
     * @return el consumo guardado; error {@link NotFoundException} si la tarjeta no existe o no
     *     pertenece al cliente; {@link BusinessRuleException} si el monto no es válido o el consumo
     *     supera el crédito disponible
     */
    Single<Charge> charge(
            String customerId, String creditCardId, BigDecimal amount, LocalDate date);
}
