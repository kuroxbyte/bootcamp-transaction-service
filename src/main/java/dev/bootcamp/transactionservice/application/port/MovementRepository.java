package dev.bootcamp.transactionservice.application.port;

import dev.bootcamp.transactionservice.domain.model.movement.Movement;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

public interface MovementRepository {
    long countByAccountIdAndMonth(String accountId, YearMonth month);

    /**
     * Saldo de la cuenta: suma de sus depósitos menos suma de sus retiros (0 si no tiene movimientos).
     */
    BigDecimal getBalance(String accountId);

    /** Depósitos y retiros de la cuenta. */
    List<Movement> findByAccountId(String accountId);

    Movement save(Movement movement);
}
