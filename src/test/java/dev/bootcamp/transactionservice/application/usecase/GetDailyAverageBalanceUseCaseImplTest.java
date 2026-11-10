package dev.bootcamp.transactionservice.application.usecase;

import dev.bootcamp.transactionservice.application.port.AccountClient;
import dev.bootcamp.transactionservice.application.port.MovementRepository;
import dev.bootcamp.transactionservice.domain.exception.NotFoundException;
import dev.bootcamp.transactionservice.domain.model.account.Account;
import dev.bootcamp.transactionservice.domain.model.account.SavingsMovementPolicy;
import dev.bootcamp.transactionservice.domain.model.movement.Movement;
import dev.bootcamp.transactionservice.domain.model.movement.MovementType;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import org.junit.jupiter.api.BeforeEach;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetDailyAverageBalanceUseCaseImplTest {

    private static final String CUSTOMER_ID = "100";
    private static final YearMonth NOVEMBER = YearMonth.of(2026, 11);
    private static final Account VIP_SAVINGS = new Account(
            "1",
            CUSTOMER_ID,
            new SavingsMovementPolicy(5),
            new BigDecimal("400"),
            LocalDate.of(2026, 11, 11),
            new BigDecimal("500"),
            null);

    @Mock private AccountClient accountClient;

    @Mock private MovementRepository movementRepository;

    private GetDailyAverageBalanceUseCaseImpl useCase;

    @BeforeEach
    void createUseCase() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-15T12:00:00Z"), ZoneOffset.UTC);
        useCase = new GetDailyAverageBalanceUseCaseImpl(accountClient, movementRepository, clock);
    }

    @Test
    void calculatesAverageUntilTodayWithTheAccountMovements() {
        when(accountClient.findById("1")).thenReturn(Maybe.just(VIP_SAVINGS));
        when(movementRepository.findByAccountId("1"))
                .thenReturn(Flowable.just(new Movement(
                        "1",
                        MovementType.DEPOSIT,
                        new BigDecimal("500"),
                        LocalDate.of(2026, 11, 13))));

        // Días 11 a 15: 400, 400, 900, 900, 900.
        useCase.execute(CUSTOMER_ID, "1", NOVEMBER).test()
                .assertValue(average -> average.days() == 5
                        && average.average().compareTo(new BigDecimal("700")) == 0
                        && average.meetsMinimum());
    }

    @Test
    void rejectsUnknownAccount() {
        when(accountClient.findById("9")).thenReturn(Maybe.empty());

        useCase.execute(CUSTOMER_ID, "9", NOVEMBER).test().assertError(NotFoundException.class);
    }

    @Test
    void rejectsAccountOfAnotherCustomer() {
        when(accountClient.findById("1")).thenReturn(Maybe.just(VIP_SAVINGS));

        useCase.execute("other", "1", NOVEMBER).test().assertError(NotFoundException.class);
        verify(movementRepository, never()).findByAccountId(any());
    }
}
