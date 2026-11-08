package dev.bootcamp.transactionservice.domain.model.history;

/**
 * Tipo de movimiento de un producto bancario.
 */
public enum ProductMovementType {
    /**
     * Depósito en una cuenta bancaria.
     */
    DEPOSIT,
    /**
     * Retiro de una cuenta bancaria.
     */
    WITHDRAWAL,
    /**
     * Consumo cargado a una tarjeta de crédito.
     */
    CHARGE,
    /**
     * Pago a un crédito o a una tarjeta de crédito.
     */
    PAYMENT
}
