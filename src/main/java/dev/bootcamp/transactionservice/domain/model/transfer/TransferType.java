package dev.bootcamp.transactionservice.domain.model.transfer;

/**
 * Tipo de transferencia según de quién es la cuenta destino.
 */
public enum TransferType {
    /**
     * Entre cuentas del mismo cliente.
     */
    OWN,
    /**
     * A una cuenta de otro cliente del mismo banco.
     */
    THIRD_PARTY
}
