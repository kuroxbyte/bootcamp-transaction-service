package dev.bootcamp.transactionservice.domain.model.payment;

import dev.bootcamp.transactionservice.domain.exception.BusinessRuleException;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Pago realizado a un producto de crédito: un crédito (personal o empresarial) o una tarjeta de
 * crédito.
 *
 * @param target          producto de crédito pagado
 * @param productId       id del crédito o de la tarjeta en credit-service, según {@code target}
 * @param payerCustomerId cliente que hizo el pago: el dueño del producto o un tercero
 */
public record Payment(
        PaymentTarget target,
        String productId,
        BigDecimal amount,
        LocalDate date,
        String payerCustomerId) {

    /**
     * Valida que el monto del pago sea mayor a 0.
     */
    public Payment {
        if (amount == null || amount.signum() <= 0) {
            throw new BusinessRuleException("El monto del pago debe ser mayor a 0");
        }
    }
}
