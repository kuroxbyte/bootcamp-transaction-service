package dev.bootcamp.transactionservice.domain.model.credit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CreditTest {

    @Test
    void belongsToItsCustomerOnly() {
        Credit credit = new Credit(1L, 100L);

        assertThat(credit.belongsTo(100L)).isTrue();
        assertThat(credit.belongsTo(200L)).isFalse();
    }
}
