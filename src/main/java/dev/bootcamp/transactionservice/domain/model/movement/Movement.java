package dev.bootcamp.transactionservice.domain.model.movement;

import dev.bootcamp.transactionservice.domain.exception.BusinessRuleException;
import java.math.BigDecimal;
import java.time.LocalDate;

public record Movement(Long accountId, MovementType type, BigDecimal amount, LocalDate date) {

    public Movement {
        if (amount == null || amount.signum() <= 0) {
            throw new BusinessRuleException("El monto debe ser mayor a 0");
        }
    }
}
