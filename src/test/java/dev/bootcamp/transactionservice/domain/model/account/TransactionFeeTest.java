package dev.bootcamp.transactionservice.domain.model.account;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionFeeTest {

    private final TransactionFee fee = new TransactionFee(3, new BigDecimal("2.00"));

    @Test
    void transactionsWithinFreeOnesPayNothing() {
        assertThat(fee.commissionFor(0)).isEqualByComparingTo("0");
        assertThat(fee.commissionFor(2)).isEqualByComparingTo("0");
    }

    @Test
    void eachTransactionAfterFreeOnesPaysFixedAmount() {
        assertThat(fee.commissionFor(3)).isEqualByComparingTo("2.00");
        assertThat(fee.commissionFor(10)).isEqualByComparingTo("2.00");
    }

    @Test
    void withoutFreeTransactionsEveryTransactionPays() {
        assertThat(new TransactionFee(0, BigDecimal.ONE).commissionFor(0))
                .isEqualByComparingTo("1");
    }
}
