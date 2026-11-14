package dev.bootcamp.transactionservice.infrastructure.scheduling;

import dev.bootcamp.transactionservice.application.usecase.ChargeMonthlyFeesUseCase;
import dev.bootcamp.transactionservice.domain.model.movement.Movement;
import dev.bootcamp.transactionservice.domain.model.movement.MovementType;
import io.reactivex.rxjava3.core.Flowable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MonthlyFeeChargeTest {

    private static final Clock DECEMBER_FIRST =
            Clock.fixed(Instant.parse("2026-12-01T05:15:00Z"), ZoneOffset.UTC);

    @Mock private ChargeMonthlyFeesUseCase useCase;

    @Test
    void chargesThePreviousMonth() {
        when(useCase.execute(YearMonth.of(2026, 11))).thenReturn(Flowable.just(new Movement(
                "c1",
                MovementType.MAINTENANCE_FEE,
                BigDecimal.TEN,
                LocalDate.of(2026, 12, 1))));

        new MonthlyFeeCharge(useCase, DECEMBER_FIRST).chargePreviousMonth();

        verify(useCase).execute(YearMonth.of(2026, 11));
    }

    @Test
    void logsInsteadOfFailingWhenTheChargeFails() {
        when(useCase.execute(YearMonth.of(2026, 11)))
                .thenReturn(Flowable.error(new IllegalStateException("account-service down")));

        assertThatCode(() -> new MonthlyFeeCharge(useCase, DECEMBER_FIRST).chargePreviousMonth())
                .doesNotThrowAnyException();
    }
}
