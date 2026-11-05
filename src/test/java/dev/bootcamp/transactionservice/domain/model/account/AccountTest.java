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

    private static Movement movementOn(int day) {
        return new Movement(1L, MovementType.DEPOSIT, BigDecimal.TEN, LocalDate.of(2026, 11, day));
    }

    @Test
    void savingsAllowsMovementsBelowMonthlyLimit() {
        Account savings = new Account(1L, 100L, AccountType.SAVINGS, 2, null);

        assertThatCode(() -> savings.validateMovement(movementOn(1), 1)).doesNotThrowAnyException();
    }

    @Test
    void savingsRejectsMovementOverMonthlyLimit() {
        Account savings = new Account(1L, 100L, AccountType.SAVINGS, 2, null);

        assertThatThrownBy(() -> savings.validateMovement(movementOn(3), 2))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void checkingAllowsUnlimitedMovements() {
        Account checking = new Account(1L, 100L, AccountType.CHECKING, null, null);

        assertThatCode(() -> checking.validateMovement(movementOn(1), 1_000)).doesNotThrowAnyException();
    }

    @Test
    void fixedTermAllowsOneMovementOnConfiguredDay() {
        Account fixedTerm = new Account(1L, 100L, AccountType.FIXED_TERM, null, 15);

        assertThatCode(() -> fixedTerm.validateMovement(movementOn(15), 0)).doesNotThrowAnyException();
    }

    @Test
    void fixedTermRejectsMovementOnOtherDay() {
        Account fixedTerm = new Account(1L, 100L, AccountType.FIXED_TERM, null, 15);

        assertThatThrownBy(() -> fixedTerm.validateMovement(movementOn(14), 0))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void fixedTermRejectsSecondMovementInMonth() {
        Account fixedTerm = new Account(1L, 100L, AccountType.FIXED_TERM, null, 15);

        assertThatThrownBy(() -> fixedTerm.validateMovement(movementOn(15), 1))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void belongsToItsCustomerOnly() {
        Account account = new Account(1L, 100L, AccountType.CHECKING, null, null);

        assertThat(account.belongsTo(100L)).isTrue();
        assertThat(account.belongsTo(200L)).isFalse();
    }
}
