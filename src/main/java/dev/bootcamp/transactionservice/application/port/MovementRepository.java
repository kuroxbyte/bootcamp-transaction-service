package dev.bootcamp.transactionservice.application.port;

import dev.bootcamp.transactionservice.domain.model.movement.Movement;
import java.time.YearMonth;

public interface MovementRepository {
    long countByAccountIdAndMonth(Long accountId, YearMonth month);

    Movement save(Movement movement);
}
