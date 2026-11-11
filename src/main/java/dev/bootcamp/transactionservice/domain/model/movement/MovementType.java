package dev.bootcamp.transactionservice.domain.model.movement;

/**
 * Tipo de movimiento de una cuenta bancaria: depósito, retiro o transferencia.
 */
public enum MovementType {
    /**
     * Depósito en la cuenta.
     */
    DEPOSIT,
    /**
     * Retiro de la cuenta.
     */
    WITHDRAWAL,
    /**
     * Transferencia recibida de otra cuenta del banco; se trata como un depósito.
     */
    TRANSFER_IN,
    /**
     * Transferencia enviada a otra cuenta del banco; se trata como un retiro.
     */
    TRANSFER_OUT;

    /**
     * Indica si el movimiento suma al saldo (depósito o transferencia recibida).
     */
    public boolean addsToBalance() {
        return this == DEPOSIT || this == TRANSFER_IN;
    }
}
