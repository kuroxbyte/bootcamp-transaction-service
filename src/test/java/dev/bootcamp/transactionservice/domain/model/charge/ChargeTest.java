package dev.bootcamp.transactionservice.domain.model.charge;

import dev.bootcamp.transactionservice.domain.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChargeTest {

    private static final LocalDate DATE = LocalDate.of(2026, 11, 15);

    @Test
    void acceptsPositiveAmount() {
        assertThatCode(() -> new Charge("1", BigDecimal.ONE, DATE)).doesNotThrowAnyException();
    }

    @Test
    void rejectsZeroNegativeOrMissingAmount() {
        assertThatThrownBy(() -> new Charge("1", BigDecimal.ZERO, DATE))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> new Charge("1", new BigDecimal("-1"), DATE))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> new Charge("1", null, DATE))
                .isInstanceOf(BusinessRuleException.class);
    }
}
