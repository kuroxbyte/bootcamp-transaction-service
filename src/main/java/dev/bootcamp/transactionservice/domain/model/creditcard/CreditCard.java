package dev.bootcamp.transactionservice.domain.model.creditcard;

import dev.bootcamp.transactionservice.domain.exception.BusinessRuleException;
import dev.bootcamp.transactionservice.domain.model.charge.Charge;

import java.math.BigDecimal;

/**
 * Vista local de una tarjeta de crédito, obtenida de credit-service.
 */
public record CreditCard(String id, String customerId, BigDecimal creditLimit) {

    /**
     * Indica si la tarjeta pertenece al cliente.
     */
    public boolean belongsTo(String customerId) {
        return this.customerId.equals(customerId);
    }

    /**
     * Crédito disponible: límite de crédito menos la deuda actual.
     *
     * @param debt deuda actual de la tarjeta (consumos menos pagos)
     */
    public BigDecimal availableCredit(BigDecimal debt) {
        return creditLimit.subtract(debt);
    }

    /**
     * Valida que el consumo quepa en el crédito disponible de la tarjeta.
     *
     * <p>No modifica nada: si el consumo es válido el método termina sin efecto y quien lo invoca
     * debe guardarlo; si no, lanza una excepción. Un consumo por el crédito disponible exacto sí se
     * permite y deja la tarjeta sin crédito disponible.
     *
     * <p>Ejemplo: con un límite de 1000 y una deuda de 800, el disponible es 200; se acepta un
     * consumo de 200 y se rechaza uno de 200.01.
     *
     * @param charge consumo a validar
     * @param debt deuda actual de la tarjeta antes del consumo (consumos menos pagos)
     * @throws BusinessRuleException si el monto del consumo supera el crédito disponible
     */
    public void validateCharge(Charge charge, BigDecimal debt) {
        if (charge.amount().compareTo(availableCredit(debt)) > 0) {
            throw new BusinessRuleException(
                    "El consumo supera el crédito disponible de la tarjeta");
        }
    }
}
