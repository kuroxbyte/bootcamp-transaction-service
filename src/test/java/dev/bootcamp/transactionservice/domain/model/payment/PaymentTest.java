package dev.bootcamp.transactionservice.domain.model.payment;

import dev.bootcamp.transactionservice.domain.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentTest {

    private static final LocalDate DATE = LocalDate.of(2026, 11, 15);

    @Test
    void acceptsPositiveAmount() {
        assertThatCode(() -> new Payment(PaymentTarget.CREDIT, "1", BigDecimal.ONE, DATE, "c1"))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsZeroNegativeOrMissingAmount() {
        assertThatThrownBy(
                () -> new Payment(PaymentTarget.CREDIT,
                "1",
                BigDecimal.ZERO,
                DATE,
                "c1"))
                .isInstanceOf(BusinessRuleException.class);
        BigDecimal negative = new BigDecimal("-1");
        assertThatThrownBy(() -> new Payment(PaymentTarget.CREDIT, "1", negative, DATE, "c1"))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> new Payment(PaymentTarget.CREDIT, "1", null, DATE, "c1"))
                .isInstanceOf(BusinessRuleException.class);
    }
}
