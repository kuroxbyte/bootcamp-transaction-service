package dev.bootcamp.transactionservice.domain.model.payment;

/** Producto al que se aplica un pago. */
public enum PaymentTarget {
    /** Crédito personal o empresarial. */
    CREDIT,
    /** Tarjeta de crédito. */
    CREDIT_CARD
}
