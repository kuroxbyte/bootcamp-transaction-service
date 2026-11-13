package dev.bootcamp.transactionservice.application.port;

import dev.bootcamp.transactionservice.domain.model.movement.Movement;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * Puerto de persistencia de los depósitos y retiros de cuentas bancarias.
 */
public interface MovementRepository {

    /**
     * Cantidad de movimientos de la cuenta en el mes indicado, sin contar las devoluciones de pagos
     * Yanki (no son transacciones del cliente).
     */
    Single<Long> countByAccountIdAndMonth(String accountId, YearMonth month);

    /**
     * Saldo de los movimientos de la cuenta: suma de sus depósitos menos suma de sus retiros (0 si
     * no tiene movimientos). No incluye el monto de apertura.
     */
    Single<BigDecimal> getBalance(String accountId);

    /**
     * Depósitos, retiros y transferencias de la cuenta.
     */
    Flowable<Movement> findByAccountId(String accountId);

    /**
     * Pagos hechos con la tarjeta de débito, de cualquier cuenta.
     */
    Flowable<Movement> findByDebitCardId(String debitCardId);

    /**
     * Guarda el depósito o retiro.
     */
    Single<Movement> save(Movement movement);

    /**
     * Guarda varios movimientos en una sola operación (los dos de una transferencia).
     */
    Flowable<Movement> saveAll(List<Movement> movements);

    /**
     * Depósitos, retiros y transferencias de todo el banco con fecha entre {@code from} y
     * {@code to}, ambos incluidos.
     */
    Flowable<Movement> findByDateBetween(LocalDate from, LocalDate to);
}
