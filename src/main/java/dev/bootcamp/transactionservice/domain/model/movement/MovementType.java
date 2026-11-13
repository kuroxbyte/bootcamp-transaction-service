package dev.bootcamp.transactionservice.domain.model.movement;

/**
 * Tipo de movimiento de una cuenta bancaria: depósito, retiro, transferencia o pago con tarjeta de
 * débito.
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
    TRANSFER_OUT,
    /**
     * Pago con tarjeta de débito desde la cuenta principal de la tarjeta; se trata como un retiro.
     */
    DEBIT_CARD_PAYMENT,
    /**
     * Pago Yanki enviado desde la cuenta principal de la tarjeta del monedero; se trata como un
     * retiro.
     */
    YANKI_PAYMENT_SENT,
    /**
     * Pago Yanki recibido en la cuenta principal de la tarjeta del monedero (o el saldo del
     * monedero transferido al asociar la tarjeta); se trata como un depósito.
     */
    YANKI_PAYMENT_RECEIVED,
    /**
     * Devolución de un pago Yanki enviado que no llegó a su destino; suma al saldo sin reglas ni
     * comisión.
     */
    YANKI_PAYMENT_REVERSAL;

    /**
     * Indica si el movimiento suma al saldo (depósito, transferencia o pago Yanki recibido, o
     * devolución de un pago Yanki).
     */
    public boolean addsToBalance() {
        return this == DEPOSIT
                || this == TRANSFER_IN
                || this == YANKI_PAYMENT_RECEIVED
                || this == YANKI_PAYMENT_REVERSAL;
    }
}
