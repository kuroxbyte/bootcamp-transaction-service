package dev.bootcamp.transactionservice.domain.model.movement;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.bootcamp.transactionservice.domain.exception.BusinessRuleException;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class MovementTest {

    private static final LocalDate DATE = LocalDate.of(2026, 11, 15);

    @Test
    void acceptsPositiveAmount() {
        assertThatCode(() -> new Movement(1L, MovementType.DEPOSIT, BigDecimal.ONE, DATE))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsZeroNegativeOrMissingAmount() {
        assertThatThrownBy(() -> new Movement(1L, MovementType.DEPOSIT, BigDecimal.ZERO, DATE))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> new Movement(1L, MovementType.WITHDRAWAL, new BigDecimal("-5"), DATE))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> new Movement(1L, MovementType.DEPOSIT, null, DATE))
                .isInstanceOf(BusinessRuleException.class);
    }
}
