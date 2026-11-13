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

    @Test
    void transfersAffectBalanceLikeDepositsAndWithdrawals() {
        assertThat(new Movement("1", MovementType.TRANSFER_IN, BigDecimal.TEN, DATE)
                .withCommission(BigDecimal.ONE).balanceEffect()).isEqualByComparingTo("9");
        assertThat(new Movement("1", MovementType.TRANSFER_OUT, BigDecimal.TEN, DATE)
                .withCommission(BigDecimal.ONE).balanceEffect()).isEqualByComparingTo("-11");
    }

    @Test
    void debitCardPaymentKeepsItsCardAndActsLikeWithdrawal() {
        Movement payment = Movement.debitCardPayment("a1", "d1", BigDecimal.TEN, DATE)
                .withCommission(BigDecimal.ONE);

        assertThat(payment.type()).isEqualTo(MovementType.DEBIT_CARD_PAYMENT);
        assertThat(payment.debitCardId()).isEqualTo("d1");
        assertThat(payment.balanceEffect()).isEqualByComparingTo("-11");
    }

    @Test
    void yankiPaymentsActLikeWithdrawalOrDepositAndReversalAddsBack() {
        assertThat(new Movement("1", MovementType.YANKI_PAYMENT_SENT, BigDecimal.TEN, DATE)
                .withCommission(BigDecimal.ONE).balanceEffect()).isEqualByComparingTo("-11");
        assertThat(new Movement("1", MovementType.YANKI_PAYMENT_RECEIVED, BigDecimal.TEN, DATE)
                .withCommission(BigDecimal.ONE).balanceEffect()).isEqualByComparingTo("9");
        assertThat(new Movement("1", MovementType.YANKI_PAYMENT_REVERSAL, BigDecimal.TEN, DATE)
                .balanceEffect()).isEqualByComparingTo("10");
    }
}
