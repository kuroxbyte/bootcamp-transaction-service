package dev.bootcamp.transactionservice.domain.model.account;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.bootcamp.transactionservice.domain.exception.BusinessRuleException;
import dev.bootcamp.transactionservice.domain.model.movement.Movement;
import dev.bootcamp.transactionservice.domain.model.movement.MovementType;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class FixedTermMovementPolicyTest {

    private final MovementPolicy policy = new FixedTermMovementPolicy(15);

    private static Movement movementOn(int day) {
        return new Movement(1L, MovementType.WITHDRAWAL, BigDecimal.TEN, LocalDate.of(2026, 11, day));
    }

    @Test
    void allowsOneMovementOnConfiguredDay() {
        assertThatCode(() -> policy.validate(movementOn(15), 0)).doesNotThrowAnyException();
    }

    @Test
    void rejectsMovementOnOtherDay() {
        assertThatThrownBy(() -> policy.validate(movementOn(14), 0))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void rejectsSecondMovementInMonth() {
        assertThatThrownBy(() -> policy.validate(movementOn(15), 1))
                .isInstanceOf(BusinessRuleException.class);
    }
}
