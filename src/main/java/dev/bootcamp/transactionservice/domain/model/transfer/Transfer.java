package dev.bootcamp.transactionservice.domain.model.transfer;

import dev.bootcamp.transactionservice.domain.exception.BusinessRuleException;
import dev.bootcamp.transactionservice.domain.exception.NotFoundException;
import dev.bootcamp.transactionservice.domain.model.account.Account;
import dev.bootcamp.transactionservice.domain.model.movement.Movement;
import dev.bootcamp.transactionservice.domain.model.movement.MovementType;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Transferencia entre dos cuentas del banco: una transferencia enviada en la cuenta origen (se
 * trata como un retiro) y una recibida en la cuenta destino (se trata como un depósito). Cada
 * movimiento lleva la comisión de su propia cuenta.
 *
 * @param type     propia o a un tercero
 * @param outgoing movimiento {@link MovementType#TRANSFER_OUT} de la cuenta origen
 * @param incoming movimiento {@link MovementType#TRANSFER_IN} de la cuenta destino
 */
public record Transfer(TransferType type, Movement outgoing, Movement incoming) {

    /**
     * Crea los dos movimientos de una transferencia, todavía sin comisión, validando las cuentas.
     *
     * <ul>
     *   <li>La cuenta origen debe pertenecer al cliente.
     *   <li>Origen y destino deben ser cuentas distintas.
     *   <li>{@link TransferType#OWN}: la cuenta destino debe ser del mismo cliente.
     *   <li>{@link TransferType#THIRD_PARTY}: la cuenta destino debe ser de otro cliente.
     * </ul>
     *
     * <p>Las reglas de cada cuenta (límites, día de plazo fijo, comisión y saldo) se aplican
     * después con {@link Account#prepareMovement} sobre cada movimiento.
     *
     * @param customerId id del cliente que transfiere
     * @throws NotFoundException     si la cuenta origen no pertenece al cliente
     * @throws BusinessRuleException si origen y destino son la misma cuenta, el monto no es mayor a
     *     0 o el destino no corresponde al tipo de transferencia
     */
    public static Transfer between(
            TransferType type,
            String customerId,
            Account source,
            Account target,
            BigDecimal amount,
            LocalDate date) {
        if (!source.belongsTo(customerId)) {
            throw new NotFoundException("La cuenta origen no pertenece al cliente");
        }
        if (source.id().equals(target.id())) {
            throw new BusinessRuleException("La cuenta destino debe ser distinta de la de origen");
        }
        boolean ownTarget = target.belongsTo(customerId);
        if (type == TransferType.OWN && !ownTarget) {
            throw new BusinessRuleException(
                    "La cuenta destino no es del cliente; use la transferencia a terceros");
        }
        if (type == TransferType.THIRD_PARTY && ownTarget) {
            throw new BusinessRuleException(
                    "La cuenta destino es del cliente; use la transferencia entre cuentas propias");
        }
        return new Transfer(
                type,
                new Movement(source.id(), MovementType.TRANSFER_OUT, amount, date),
                new Movement(target.id(), MovementType.TRANSFER_IN, amount, date));
    }

    /**
     * Copia de la transferencia con los movimientos ya validados y con su comisión.
     */
    public Transfer withMovements(Movement outgoing, Movement incoming) {
        return new Transfer(type, outgoing, incoming);
    }
}
