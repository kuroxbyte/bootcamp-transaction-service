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
     * Pago Yanki enviado desde una cuenta bancaria.
     */
    YANKI_PAYMENT_SENT,
    /**
     * Pago Yanki recibido en una cuenta bancaria.
     */
    YANKI_PAYMENT_RECEIVED,
    /**
     * Devolución de un pago Yanki que no llegó a su destino.
     */
    YANKI_PAYMENT_REVERSAL,
    /**
     * Comisión de mantenimiento mensual de una cuenta corriente.
     */
    MAINTENANCE_FEE,
    /**
     * Comisión mensual de una cuenta de ahorro VIP que no alcanzó su promedio diario mínimo.
     */
    MINIMUM_AVERAGE_FEE,
    /**
     * Consumo cargado a una tarjeta de crédito.
     */
    CHARGE,
    /**
     * Pago a un crédito o a una tarjeta de crédito.
     */
    PAYMENT
}
