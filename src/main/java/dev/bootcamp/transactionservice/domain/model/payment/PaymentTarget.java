package dev.bootcamp.transactionservice.domain.model.payment;

/**
 * Producto de crédito al que se aplica un pago: un crédito (personal o empresarial) o una tarjeta
 * de crédito.
 */
public enum PaymentTarget {
    /** Crédito personal o empresarial. */
    CREDIT,
    /** Tarjeta de crédito. */
    CREDIT_CARD
}
