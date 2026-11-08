package dev.bootcamp.transactionservice.domain.model.charge;

import dev.bootcamp.transactionservice.domain.exception.BusinessRuleException;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Consumo cargado a una tarjeta de crédito.
 */
public record Charge(String creditCardId, BigDecimal amount, LocalDate date) {

    /**
     * Valida que el monto del consumo sea mayor a 0.
     */
    public Charge {
        if (amount == null || amount.signum() <= 0) {
            throw new BusinessRuleException("El monto del consumo debe ser mayor a 0");
        }
    }
}
