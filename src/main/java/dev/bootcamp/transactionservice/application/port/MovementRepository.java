package dev.bootcamp.transactionservice.application.port;

import dev.bootcamp.transactionservice.domain.model.movement.Movement;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import java.math.BigDecimal;
import java.time.YearMonth;

/** Puerto de persistencia de los depósitos y retiros de cuentas bancarias. */
public interface MovementRepository {

    /** Cantidad de movimientos (depósitos y retiros) de la cuenta en el mes indicado. */
    Single<Long> countByAccountIdAndMonth(String accountId, YearMonth month);

    /**
     * Saldo de la cuenta: suma de sus depósitos menos suma de sus retiros (0 si no tiene
     * movimientos).
     */
    Single<BigDecimal> getBalance(String accountId);

    /** Depósitos y retiros de la cuenta. */
    Flowable<Movement> findByAccountId(String accountId);

    /** Guarda el depósito o retiro. */
    Single<Movement> save(Movement movement);
}
