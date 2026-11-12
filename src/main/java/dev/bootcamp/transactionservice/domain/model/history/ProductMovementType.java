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
     * Transferencia recibida en una cuenta bancaria.
     */
    TRANSFER_IN,
    /**
     * Transferencia enviada desde una cuenta bancaria.
     */
    TRANSFER_OUT,
    /**
     * Pago con tarjeta de débito desde una cuenta bancaria.
     */
    DEBIT_CARD_PAYMENT,
    /**
     * Consumo cargado a una tarjeta de crédito.
     */
    CHARGE,
    /**
     * Pago a un crédito o a una tarjeta de crédito.
     */
    PAYMENT
}
