package dev.bootcamp.transactionservice.domain.model.movement;

import dev.bootcamp.transactionservice.domain.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MovementTest {

    private static final LocalDate DATE = LocalDate.of(2026, 11, 15);

    @Test
    void acceptsPositiveAmount() {
        assertThatCode(() -> new Movement("1", MovementType.DEPOSIT, BigDecimal.ONE, DATE))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsZeroNegativeOrMissingAmount() {
        assertThatThrownBy(() -> new Movement("1", MovementType.DEPOSIT, BigDecimal.ZERO, DATE))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(
                        () ->
                                new Movement(
                                        "1", MovementType.WITHDRAWAL, new BigDecimal("-5"), DATE))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> new Movement("1", MovementType.DEPOSIT, null, DATE))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void startsWithoutCommissionAndRejectsNegativeCommission() {
        Movement deposit = new Movement("1", MovementType.DEPOSIT, BigDecimal.TEN, DATE);

        assertThat(deposit.commission()).isEqualByComparingTo("0");
        assertThatThrownBy(() -> deposit.withCommission(new BigDecimal("-1")))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> deposit.withCommission(null))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void commissionIsDiscountedFromBalanceEffect() {
        BigDecimal commission = new BigDecimal("2");

        assertThat(new Movement("1", MovementType.DEPOSIT, BigDecimal.TEN, DATE)
                .withCommission(commission).balanceEffect()).isEqualByComparingTo("8");
        assertThat(new Movement("1", MovementType.WITHDRAWAL, BigDecimal.TEN, DATE)
                .withCommission(commission).balanceEffect()).isEqualByComparingTo("-12");
    }
}
