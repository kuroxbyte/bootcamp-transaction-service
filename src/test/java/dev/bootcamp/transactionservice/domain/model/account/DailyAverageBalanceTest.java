package dev.bootcamp.transactionservice.domain.model.account;

import dev.bootcamp.transactionservice.domain.exception.BusinessRuleException;
import dev.bootcamp.transactionservice.domain.model.movement.Movement;
import dev.bootcamp.transactionservice.domain.model.movement.MovementType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DailyAverageBalanceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 15);
    private static final BigDecimal VIP_MINIMUM = new BigDecimal("500");

    private static Account vipSavings(String openingAmount, LocalDate openedAt) {
        return new Account(
                "1",
                "100",
                new SavingsMovementPolicy(5),
                new BigDecimal(openingAmount),
                openedAt,
                VIP_MINIMUM);
    }

    private static Movement movement(MovementType type, String amount, LocalDate date) {
        return new Movement("1", type, new BigDecimal(amount), date);
    }

    @Test
    void averagesEndOfDayBalancesFromOpeningUntilToday() {
        Account account = vipSavings("300", LocalDate.of(2026, 11, 10));
        List<Movement> movements = List.of(
                movement(MovementType.DEPOSIT, "600", LocalDate.of(2026, 11, 12)),
                movement(MovementType.WITHDRAWAL, "100", LocalDate.of(2026, 11, 14)));

        DailyAverageBalance average =
                account.dailyAverageBalance(YearMonth.of(2026, 11), TODAY, movements);

        // Días 10 a 15: 300, 300, 900, 900, 800, 800.
        assertThat(average.days()).isEqualTo(6);
        assertThat(average.average()).isEqualByComparingTo("666.67");
        assertThat(average.minimumDailyAverage()).isEqualByComparingTo("500");
        assertThat(average.meetsMinimum()).isTrue();
    }

    @Test
    void averagesWholePastMonthAndIgnoresLaterMovements() {
        Account account = vipSavings("0", LocalDate.of(2026, 9, 20));
        List<Movement> movements = List.of(
                movement(MovementType.DEPOSIT, "100", LocalDate.of(2026, 9, 25)),
                movement(MovementType.DEPOSIT, "310", LocalDate.of(2026, 10, 31)),
                movement(MovementType.DEPOSIT, "5000", LocalDate.of(2026, 11, 2)));

        DailyAverageBalance average =
                account.dailyAverageBalance(YearMonth.of(2026, 10), TODAY, movements);

        // 30 días con 100 y el día 31 con 410.
        assertThat(average.days()).isEqualTo(31);
        assertThat(average.average()).isEqualByComparingTo("110.00");
        assertThat(average.meetsMinimum()).isFalse();
    }

    @Test
    void accountWithoutMinimumDoesNotSayIfItMeetsIt() {
        Account checking = new Account(
                "1",
                "100",
                new CheckingMovementPolicy(),
                new BigDecimal("50"),
                LocalDate.of(2026, 11, 1),
                null);

        DailyAverageBalance average =
                checking.dailyAverageBalance(YearMonth.of(2026, 11), TODAY, List.of());

        assertThat(average.average()).isEqualByComparingTo("50");
        assertThat(average.minimumDailyAverage()).isNull();
        assertThat(average.meetsMinimum()).isNull();
    }

    @Test
    void rejectsMonthBeforeOpeningOrAfterToday() {
        Account account = vipSavings("0", LocalDate.of(2026, 11, 10));

        assertThatThrownBy(
                        () -> account.dailyAverageBalance(YearMonth.of(2026, 10), TODAY, List.of()))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(
                        () -> account.dailyAverageBalance(YearMonth.of(2026, 12), TODAY, List.of()))
                .isInstanceOf(BusinessRuleException.class);
    }
}
