package dev.bootcamp.transactionservice.domain.model.creditcard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.bootcamp.transactionservice.domain.exception.BusinessRuleException;
import dev.bootcamp.transactionservice.domain.model.charge.Charge;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class CreditCardTest {

    private final CreditCard card = new CreditCard("1", "100", new BigDecimal("1000"));

    private static Charge charge(String amount) {
        return new Charge("1", new BigDecimal(amount), LocalDate.of(2026, 11, 15));
    }

    @Test
    void belongsToItsCustomerOnly() {
        assertThat(card.belongsTo("100")).isTrue();
        assertThat(card.belongsTo("200")).isFalse();
    }

    @Test
    void availableCreditIsLimitMinusDebt() {
        assertThat(card.availableCredit(new BigDecimal("800"))).isEqualByComparingTo("200");
    }

    @Test
    void allowsChargeUpToAvailableCredit() {
        assertThatCode(() -> card.validateCharge(charge("150"), new BigDecimal("800")))
                .doesNotThrowAnyException();
        assertThatCode(() -> card.validateCharge(charge("200"), new BigDecimal("800")))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsChargeOverAvailableCredit() {
        assertThatThrownBy(() -> card.validateCharge(charge("200.01"), new BigDecimal("800")))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void rejectsChargeOverLimitWithoutDebt() {
        assertThatThrownBy(() -> card.validateCharge(charge("1000.01"), BigDecimal.ZERO))
                .isInstanceOf(BusinessRuleException.class);
    }
}
