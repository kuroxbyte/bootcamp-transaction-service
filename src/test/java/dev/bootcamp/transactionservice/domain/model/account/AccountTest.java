package dev.bootcamp.transactionservice.domain.model.account;

import dev.bootcamp.transactionservice.domain.exception.BusinessRuleException;
import dev.bootcamp.transactionservice.domain.model.movement.Movement;
import dev.bootcamp.transactionservice.domain.model.movement.MovementType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountTest {

    /**
     * Sin comisión: todas las transacciones del mes son gratuitas.
     */
    private static final TransactionFee FREE =
            new TransactionFee(Integer.MAX_VALUE, BigDecimal.ZERO);

    private static final LocalDate DATE = LocalDate.of(2026, 11, 15);

    private static Movement withdrawal(String amount) {
        return new Movement("1", MovementType.WITHDRAWAL, new BigDecimal(amount), DATE);
    }

    private static Account checking() {
        return new Account(
                "1",
                "100",
                new CheckingMovementPolicy(),
                BigDecimal.ZERO,
                null,
                null,
                FREE);
    }

    @Test
    void belongsToItsCustomerOnly() {
        assertThat(checking().belongsTo("100")).isTrue();
        assertThat(checking().belongsTo("200")).isFalse();
    }

    @Test
    void appliesMovementPolicyOfAccountType() {
        Account savings = new Account(
                "1",
                "100",
                new SavingsMovementPolicy(2),
                BigDecimal.ZERO,
                null,
                null,
                FREE);
        Movement deposit = new Movement("1", MovementType.DEPOSIT, BigDecimal.TEN, DATE);

        assertThatThrownBy(() -> savings.prepareMovement(deposit, 2, BigDecimal.ZERO))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void allowsWithdrawalUpToBalance() {
        assertThatCode(
                        () ->
                                checking()
                                        .prepareMovement(
                                                withdrawal("50"), 0, new BigDecimal("100")))
                .doesNotThrowAnyException();
        assertThatCode(
                        () ->
                                checking()
                                        .prepareMovement(
                                                withdrawal("100"), 0, new BigDecimal("100")))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsWithdrawalOverBalanceInEveryAccountType() {
        Account[] accounts = {
            new Account(
                    "1",
                    "100",
                    new SavingsMovementPolicy(5),
                    BigDecimal.ZERO,
                    null,
                    null,
                    FREE),
            checking(),
            new Account(
                    "1",
                    "100",
                    new FixedTermMovementPolicy(15),
                    BigDecimal.ZERO,
                    null,
                    null,
                    FREE)
        };

        for (Account account : accounts) {
            assertThatThrownBy(
                            () ->
                                    account.prepareMovement(
                                            withdrawal("100.01"), 0, new BigDecimal("100")))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Saldo insuficiente para el retiro y su comisión");
        }
    }

    @Test
    void depositDoesNotDependOnBalance() {
        Movement deposit = new Movement("1", MovementType.DEPOSIT, BigDecimal.TEN, DATE);

        assertThatCode(() -> checking().prepareMovement(deposit, 0, BigDecimal.ZERO))
                .doesNotThrowAnyException();
    }

    @Test
    void balanceIsOpeningAmountPlusMovements() {
        Account account = new Account(
                "1",
                "100",
                new CheckingMovementPolicy(),
                new BigDecimal("200"),
                null,
                null,
                FREE);

        assertThat(account.balance(new BigDecimal("-50.25"))).isEqualByComparingTo("149.75");
    }

    @Test
    void chargesCommissionOnlyAfterFreeTransactionsOfTheMonth() {
        Account account = new Account(
                "1",
                "100",
                new CheckingMovementPolicy(),
                BigDecimal.ZERO,
                null,
                null,
                new TransactionFee(3, new BigDecimal("2.00")));
        Movement deposit = new Movement("1", MovementType.DEPOSIT, new BigDecimal("100"), DATE);

        assertThat(account.prepareMovement(deposit, 2, BigDecimal.ZERO).commission())
                .isEqualByComparingTo("0");
        assertThat(account.prepareMovement(deposit, 3, BigDecimal.ZERO).commission())
                .isEqualByComparingTo("2.00");
        assertThat(account.prepareMovement(deposit, 7, BigDecimal.ZERO).commission())
                .isEqualByComparingTo("2.00");
    }

    @Test
    void withdrawalWithCommissionMustBeCoveredByBalance() {
        Account account = new Account(
                "1",
                "100",
                new CheckingMovementPolicy(),
                BigDecimal.ZERO,
                null,
                null,
                new TransactionFee(0, new BigDecimal("2")));

        BigDecimal balance = new BigDecimal("100");

        assertThatThrownBy(() -> account.prepareMovement(withdrawal("99"), 0, balance))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Saldo insuficiente para el retiro y su comisión");
        Movement allowed = account.prepareMovement(withdrawal("98"), 0, balance);

        assertThat(allowed.balanceEffect()).isEqualByComparingTo("-100");
    }

    @Test
    void depositSmallerThanCommissionNeedsBalanceForTheRest() {
        Account account = new Account(
                "1",
                "100",
                new CheckingMovementPolicy(),
                BigDecimal.ZERO,
                null,
                null,
                new TransactionFee(0, new BigDecimal("2")));
        Movement deposit = new Movement("1", MovementType.DEPOSIT, BigDecimal.ONE, DATE);

        assertThatThrownBy(() -> account.prepareMovement(deposit, 0, BigDecimal.ZERO))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Saldo insuficiente para la comisión del depósito");
        assertThatCode(() -> account.prepareMovement(deposit, 0, BigDecimal.ONE))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsYankiPaymentOverBalance() {
        Movement payment = new Movement("1", MovementType.YANKI_PAYMENT_SENT, BigDecimal.TEN, DATE);

        assertThatThrownBy(() -> checking().prepareMovement(payment, 0, BigDecimal.ONE))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Saldo insuficiente para el pago Yanki y su comisión");
    }
}
