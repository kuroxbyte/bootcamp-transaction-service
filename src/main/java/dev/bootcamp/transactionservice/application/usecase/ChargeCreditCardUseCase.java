package dev.bootcamp.transactionservice.application.usecase;

import dev.bootcamp.transactionservice.domain.exception.BusinessRuleException;
import dev.bootcamp.transactionservice.domain.model.charge.Charge;
import io.reactivex.rxjava3.core.Single;
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
 *
 * <p>Es reactivo: no hace nada hasta que se suscribe al {@link Single}. Si se incumple una
 * regla, el {@link Single} termina con error {@link BusinessRuleException}.
 */
public interface ChargeCreditCardUseCase {

    /**
     * Carga un consumo a una tarjeta de crédito del cliente.
     *
     * @param customerId   id del cliente que hace el consumo
     * @param creditCardId id de la tarjeta de crédito en credit-service
     * @param amount       monto del consumo
     * @param date         fecha del consumo
     * @return el consumo guardado; error {@link BusinessRuleException} si el monto no es válido, la
     *         tarjeta no existe, no pertenece al cliente o el consumo supera el crédito disponible
     */
    Single<Charge> charge(String customerId, String creditCardId, BigDecimal amount, LocalDate date);
}
