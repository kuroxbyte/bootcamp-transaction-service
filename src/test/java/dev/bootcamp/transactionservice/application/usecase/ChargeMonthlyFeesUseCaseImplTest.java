package dev.bootcamp.transactionservice.application.usecase;

import dev.bootcamp.transactionservice.application.port.AccountClient;
import dev.bootcamp.transactionservice.application.port.MovementRepository;
import dev.bootcamp.transactionservice.domain.model.account.Account;
import dev.bootcamp.transactionservice.domain.model.account.CheckingMovementPolicy;
import dev.bootcamp.transactionservice.domain.model.account.TransactionFee;
import dev.bootcamp.transactionservice.domain.model.movement.Movement;
import dev.bootcamp.transactionservice.domain.model.movement.MovementType;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChargeMonthlyFeesUseCaseImplTest {

    private static final YearMonth NOVEMBER = YearMonth.of(2026, 11);
    private static final LocalDate DECEMBER_FIRST = LocalDate.of(2026, 12, 1);

    @Mock private AccountClient accountClient;

    @Mock private MovementRepository movementRepository;

    private ChargeMonthlyFeesUseCaseImpl useCase;

    @BeforeEach
    void createUseCase() {
        useCase = new ChargeMonthlyFeesUseCaseImpl(
                accountClient, movementRepository, new BigDecimal("15"));
    }

    private static Account checking(String id, String maintenanceFee) {
        return new Account(
                id,
                "100",
                new CheckingMovementPolicy(),
                new BigDecimal("100"),
                LocalDate.of(2026, 1, 1),
                null,
                new TransactionFee(Integer.MAX_VALUE, BigDecimal.ZERO),
                new BigDecimal(maintenanceFee));
    }

    private static Movement fee(String accountId) {
        return new Movement(
                accountId, MovementType.MAINTENANCE_FEE, new BigDecimal("8"), DECEMBER_FIRST);
    }

    @Test
    void savesTheMonthlyChargesOfEachAccount() {
        when(accountClient.findAll()).thenReturn(Flowable.just(checking("c1", "8")));
        when(movementRepository.hasMonthlyCharges("c1", DECEMBER_FIRST))
                .thenReturn(Single.just(false));
        when(movementRepository.findByAccountId("c1")).thenReturn(Flowable.empty());
        when(movementRepository.saveAll(List.of(fee("c1"))))
                .thenReturn(Flowable.just(fee("c1")));

        useCase.execute(NOVEMBER).test().assertValues(fee("c1")).assertComplete();
    }

    @Test
    void skipsAccountsAlreadyChargedForTheMonth() {
        when(accountClient.findAll()).thenReturn(Flowable.just(checking("c1", "8")));
        when(movementRepository.hasMonthlyCharges("c1", DECEMBER_FIRST))
                .thenReturn(Single.just(true));

        useCase.execute(NOVEMBER).test().assertNoValues().assertComplete();
        verify(movementRepository, never()).saveAll(anyList());
    }

    @Test
    void savesNothingWhenNoChargeApplies() {
        when(accountClient.findAll()).thenReturn(Flowable.just(checking("c1", "0")));
        when(movementRepository.hasMonthlyCharges("c1", DECEMBER_FIRST))
                .thenReturn(Single.just(false));
        when(movementRepository.findByAccountId("c1")).thenReturn(Flowable.empty());

        useCase.execute(NOVEMBER).test().assertNoValues().assertComplete();
        verify(movementRepository, never()).saveAll(anyList());
    }

    @Test
    void anAccountThatFailsDoesNotStopTheOthers() {
        when(accountClient.findAll())
                .thenReturn(Flowable.just(checking("c1", "8"), checking("c2", "8")));
        when(movementRepository.hasMonthlyCharges("c1", DECEMBER_FIRST))
                .thenReturn(Single.error(new IllegalStateException("mongo down")));
        when(movementRepository.hasMonthlyCharges("c2", DECEMBER_FIRST))
                .thenReturn(Single.just(false));
        when(movementRepository.findByAccountId("c2")).thenReturn(Flowable.empty());
        when(movementRepository.saveAll(any())).thenReturn(Flowable.just(fee("c2")));

        useCase.execute(NOVEMBER).test().assertValues(fee("c2")).assertComplete();
    }
}
