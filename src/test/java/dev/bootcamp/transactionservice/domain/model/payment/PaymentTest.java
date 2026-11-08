package dev.bootcamp.transactionservice.domain.model.payment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.bootcamp.transactionservice.domain.exception.BusinessRuleException;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class PaymentTest {

    private static final LocalDate DATE = LocalDate.of(2026, 11, 15);

    @Test
    void acceptsPositiveAmount() {
        assertThatCode(() -> new Payment(PaymentTarget.CREDIT, "1", BigDecimal.ONE, DATE))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsZeroNegativeOrMissingAmount() {
        assertThatThrownBy(() -> new Payment(PaymentTarget.CREDIT, "1", BigDecimal.ZERO, DATE))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> new Payment(PaymentTarget.CREDIT, "1", new BigDecimal("-1"), DATE))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> new Payment(PaymentTarget.CREDIT, "1", null, DATE))
                .isInstanceOf(BusinessRuleException.class);
    }
}
