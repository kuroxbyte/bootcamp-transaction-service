package dev.bootcamp.transactionservice.domain.model.account;

import static org.assertj.core.api.Assertions.assertThatCode;

import dev.bootcamp.transactionservice.domain.model.movement.Movement;
import dev.bootcamp.transactionservice.domain.model.movement.MovementType;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class CheckingMovementPolicyTest {

    @Test
    void allowsUnlimitedMovements() {
        Movement movement = new Movement("1", MovementType.DEPOSIT, BigDecimal.TEN, LocalDate.of(2026, 11, 3));

        assertThatCode(() -> new CheckingMovementPolicy().validate(movement, 1_000)).doesNotThrowAnyException();
    }
}
