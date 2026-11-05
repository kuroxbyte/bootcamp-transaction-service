package dev.bootcamp.transactionservice.domain.model.payment;

import dev.bootcamp.transactionservice.domain.exception.BusinessRuleException;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Pago realizado a un producto de crédito.
 */
public record Payment(Long creditId, BigDecimal amount, LocalDate date) {

    public Payment {
        if (amount == null || amount.signum() <= 0) {
            throw new BusinessRuleException("El monto del pago debe ser mayor a 0");
        }
    }
}
