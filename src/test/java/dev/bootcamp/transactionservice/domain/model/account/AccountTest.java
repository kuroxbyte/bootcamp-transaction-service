package dev.bootcamp.transactionservice.domain.model.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.bootcamp.transactionservice.domain.exception.BusinessRuleException;
import dev.bootcamp.transactionservice.domain.model.movement.Movement;
import dev.bootcamp.transactionservice.domain.model.movement.MovementType;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class AccountTest {

    private static final LocalDate DATE = LocalDate.of(2026, 11, 15);

    private static Movement withdrawal(String amount) {
        return new Movement("1", MovementType.WITHDRAWAL, new BigDecimal(amount), DATE);
    }

    private static Account checking() {
        return new Account("1", "100", new CheckingMovementPolicy());
    }

    @Test
    void belongsToItsCustomerOnly() {
        assertThat(checking().belongsTo("100")).isTrue();
        assertThat(checking().belongsTo("200")).isFalse();
    }

    @Test
    void appliesMovementPolicyOfAccountType() {
        Account savings = new Account("1", "100", new SavingsMovementPolicy(2));
        Movement deposit = new Movement("1", MovementType.DEPOSIT, BigDecimal.TEN, DATE);

        assertThatThrownBy(() -> savings.validateMovement(deposit, 2, BigDecimal.ZERO))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void allowsWithdrawalUpToBalance() {
        assertThatCode(
                        () ->
                                checking()
                                        .validateMovement(
                                                withdrawal("50"), 0, new BigDecimal("100")))
                .doesNotThrowAnyException();
        assertThatCode(
                        () ->
                                checking()
                                        .validateMovement(
                                                withdrawal("100"), 0, new BigDecimal("100")))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsWithdrawalOverBalanceInEveryAccountType() {
        Account[] accounts = {
            new Account("1", "100", new SavingsMovementPolicy(5)),
            checking(),
            new Account("1", "100", new FixedTermMovementPolicy(15))
        };

        for (Account account : accounts) {
            assertThatThrownBy(
                            () ->
                                    account.validateMovement(
                                            withdrawal("100.01"), 0, new BigDecimal("100")))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Saldo insuficiente para el retiro");
        }
    }

    @Test
    void depositDoesNotDependOnBalance() {
        Movement deposit = new Movement("1", MovementType.DEPOSIT, BigDecimal.TEN, DATE);

        assertThatCode(() -> checking().validateMovement(deposit, 0, BigDecimal.ZERO))
                .doesNotThrowAnyException();
    }
}
