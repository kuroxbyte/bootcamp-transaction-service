package dev.bootcamp.transactionservice.domain.model.account;

import dev.bootcamp.transactionservice.domain.model.movement.Movement;
import dev.bootcamp.transactionservice.domain.model.movement.MovementType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AccountMonthlyChargesTest {

    private static final TransactionFee FREE =
            new TransactionFee(Integer.MAX_VALUE, BigDecimal.ZERO);
    private static final YearMonth NOVEMBER = YearMonth.of(2026, 11);
    private static final LocalDate DECEMBER_FIRST = LocalDate.of(2026, 12, 1);
    private static final BigDecimal MINIMUM_AVERAGE_FEE = new BigDecimal("15");

    private static Account checking(String opening, String maintenanceFee, LocalDate openedAt) {
        return new Account(
                "c1",
                "100",
                new CheckingMovementPolicy(),
                new BigDecimal(opening),
                openedAt,
                null,
                FREE,
                new BigDecimal(maintenanceFee));
    }

    private static Account vipSavings(String opening) {
        return new Account(
                "v1",
                "100",
                new SavingsMovementPolicy(5),
                new BigDecimal(opening),
                LocalDate.of(2026, 11, 1),
                new BigDecimal("500"),
                FREE);
    }

    private static Movement movement(String accountId, MovementType type, String amount, int day) {
        return new Movement(accountId, type, new BigDecimal(amount), NOVEMBER.atDay(day));
    }

    @Test
    void chargesMaintenanceFeeOnTheFirstDayOfNextMonth() {
        List<Movement> charges = checking("100", "8", LocalDate.of(2026, 10, 1))
                .monthlyCharges(NOVEMBER, List.of(), MINIMUM_AVERAGE_FEE);

        assertThat(charges).containsExactly(new Movement(
                "c1", MovementType.MAINTENANCE_FEE, new BigDecimal("8"), DECEMBER_FIRST));
    }

    @Test
    void chargesNothingWithoutMaintenanceFee() {
        assertThat(checking("100", "0", LocalDate.of(2026, 10, 1))
                .monthlyCharges(NOVEMBER, List.of(), MINIMUM_AVERAGE_FEE)).isEmpty();
    }

    @Test
    void chargesNothingIfTheAccountOpenedAfterTheMonth() {
        assertThat(checking("100", "8", LocalDate.of(2026, 12, 1))
                .monthlyCharges(NOVEMBER, List.of(), MINIMUM_AVERAGE_FEE)).isEmpty();
    }

    @Test
    void chargesOnlyWhatTheBalanceCovers() {
        Account account = checking("0", "8", LocalDate.of(2026, 10, 1));

        assertThat(account.monthlyCharges(
                NOVEMBER,
                List.of(movement("c1", MovementType.DEPOSIT, "5", 3)),
                MINIMUM_AVERAGE_FEE))
                .extracting(Movement::amount)
                .containsExactly(new BigDecimal("5"));
        assertThat(account.monthlyCharges(NOVEMBER, List.of(), MINIMUM_AVERAGE_FEE)).isEmpty();
    }

    @Test
    void chargesVipSavingsBelowItsMinimumDailyAverage() {
        // Saldo 400 todo el mes: promedio 400, menor al mínimo de 500.
        assertThat(vipSavings("400").monthlyCharges(NOVEMBER, List.of(), MINIMUM_AVERAGE_FEE))
                .containsExactly(new Movement(
                        "v1",
                        MovementType.MINIMUM_AVERAGE_FEE,
                        MINIMUM_AVERAGE_FEE,
                        DECEMBER_FIRST));
    }

    @Test
    void doesNotChargeVipSavingsThatMetItsMinimum() {
        assertThat(vipSavings("500").monthlyCharges(NOVEMBER, List.of(), MINIMUM_AVERAGE_FEE))
                .isEmpty();
    }

    @Test
    void ignoresMovementsAfterTheMonthForTheAverage() {
        // El depósito de diciembre suma al saldo disponible, no al promedio de noviembre.
        Movement december = new Movement(
                "v1", MovementType.DEPOSIT, new BigDecimal("1000"), LocalDate.of(2026, 12, 2));

        assertThat(vipSavings("400").monthlyCharges(
                NOVEMBER, List.of(december), MINIMUM_AVERAGE_FEE))
                .extracting(Movement::type)
                .containsExactly(MovementType.MINIMUM_AVERAGE_FEE);
    }

    @Test
    void ordinaryAccountsWithoutMaintenanceFeeAreNeverCharged() {
        Account savings = new Account(
                "s1",
                "100",
                new SavingsMovementPolicy(5),
                BigDecimal.TEN,
                LocalDate.of(2026, 1, 1),
                null,
                FREE,
                null);

        assertThat(savings.maintenanceFee()).isEqualByComparingTo("0");
        assertThat(savings.monthlyCharges(NOVEMBER, List.of(), MINIMUM_AVERAGE_FEE)).isEmpty();
    }
}
