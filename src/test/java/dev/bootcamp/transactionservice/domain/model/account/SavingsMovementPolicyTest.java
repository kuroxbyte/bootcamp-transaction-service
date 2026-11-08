package dev.bootcamp.transactionservice.domain.model.account;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.bootcamp.transactionservice.domain.exception.BusinessRuleException;
import dev.bootcamp.transactionservice.domain.model.movement.Movement;
import dev.bootcamp.transactionservice.domain.model.movement.MovementType;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class SavingsMovementPolicyTest {

    private final MovementPolicy policy = new SavingsMovementPolicy(2);
    private final Movement movement =
            new Movement("1", MovementType.DEPOSIT, BigDecimal.TEN, LocalDate.of(2026, 11, 3));

    @Test
    void allowsMovementsBelowMonthlyLimit() {
        assertThatCode(() -> policy.validate(movement, 0)).doesNotThrowAnyException();
        assertThatCode(() -> policy.validate(movement, 1)).doesNotThrowAnyException();
    }

    @Test
    void rejectsMovementOverMonthlyLimit() {
        assertThatThrownBy(() -> policy.validate(movement, 2))
                .isInstanceOf(BusinessRuleException.class);
    }
}
