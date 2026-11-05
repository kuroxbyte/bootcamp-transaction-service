package dev.bootcamp.transactionservice.application.port;

import dev.bootcamp.transactionservice.domain.model.movement.Movement;
import java.math.BigDecimal;
import java.time.YearMonth;

public interface MovementRepository {
    long countByAccountIdAndMonth(Long accountId, YearMonth month);

    /**
     * Saldo de la cuenta: suma de sus depósitos menos suma de sus retiros (0 si no tiene movimientos).
     */
    BigDecimal getBalance(Long accountId);

    Movement save(Movement movement);
}
