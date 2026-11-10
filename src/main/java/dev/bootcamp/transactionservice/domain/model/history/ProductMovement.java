package dev.bootcamp.transactionservice.domain.model.history;

import dev.bootcamp.transactionservice.domain.model.charge.Charge;
import dev.bootcamp.transactionservice.domain.model.movement.Movement;
import dev.bootcamp.transactionservice.domain.model.movement.MovementType;
import dev.bootcamp.transactionservice.domain.model.payment.Payment;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Movimiento de un producto bancario, en un formato común para consultar el historial de cuentas
 * bancarias, créditos y tarjetas de crédito.
 *
 * @param commission comisión cobrada; solo en depósitos y retiros, en los demás es null
 */
public record ProductMovement(
        ProductMovementType type,
        BigDecimal amount,
        LocalDate date,
        BigDecimal commission) {

    /**
     * Crea un movimiento sin comisión (pago o consumo).
     */
    public ProductMovement(ProductMovementType type, BigDecimal amount, LocalDate date) {
        this(type, amount, date, null);
    }

    /**
     * Convierte un depósito o retiro al formato común.
     */
    public static ProductMovement from(Movement movement) {
        ProductMovementType type =
                movement.type() == MovementType.DEPOSIT
                        ? ProductMovementType.DEPOSIT
                        : ProductMovementType.WITHDRAWAL;
        return new ProductMovement(type, movement.amount(), movement.date(), movement.commission());
    }

    /**
     * Convierte un pago al formato común.
     */
    public static ProductMovement from(Payment payment) {
        return new ProductMovement(ProductMovementType.PAYMENT, payment.amount(), payment.date());
    }

    /**
     * Convierte un consumo al formato común.
     */
    public static ProductMovement from(Charge charge) {
        return new ProductMovement(ProductMovementType.CHARGE, charge.amount(), charge.date());
    }
}
