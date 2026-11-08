package dev.bootcamp.transactionservice.domain.model.movement;

import dev.bootcamp.transactionservice.domain.exception.BusinessRuleException;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Depósito o retiro registrado en una cuenta bancaria. */
public record Movement(String accountId, MovementType type, BigDecimal amount, LocalDate date) {

    /** Valida que el monto del movimiento sea mayor a 0. */
    public Movement {
        if (amount == null || amount.signum() <= 0) {
            throw new BusinessRuleException("El monto debe ser mayor a 0");
        }
    }
}
