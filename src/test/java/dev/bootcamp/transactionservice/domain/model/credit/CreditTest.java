package dev.bootcamp.transactionservice.domain.model.credit;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CreditTest {

    @Test
    void belongsToItsCustomerOnly() {
        Credit credit = new Credit("1", "100");

        assertThat(credit.belongsTo("100")).isTrue();
        assertThat(credit.belongsTo("200")).isFalse();
    }
}
