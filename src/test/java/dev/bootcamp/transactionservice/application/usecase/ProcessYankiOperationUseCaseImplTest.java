package dev.bootcamp.transactionservice.application.usecase;

import dev.bootcamp.transactionservice.application.port.AccountClient;
import dev.bootcamp.transactionservice.application.port.MovementRepository;
import dev.bootcamp.transactionservice.application.port.YankiOperationRepository;
import dev.bootcamp.transactionservice.application.port.YankiOperationResultPublisher;
import dev.bootcamp.transactionservice.domain.model.account.Account;
import dev.bootcamp.transactionservice.domain.model.account.CheckingMovementPolicy;
import dev.bootcamp.transactionservice.domain.model.account.TransactionFee;
import dev.bootcamp.transactionservice.domain.model.movement.Movement;
import dev.bootcamp.transactionservice.domain.model.movement.MovementType;
import dev.bootcamp.transactionservice.domain.model.yanki.YankiOperation;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessYankiOperationUseCaseImplTest {

    private static final LocalDate DATE = LocalDate.of(2026, 11, 15);
    private static final BigDecimal AMOUNT = new BigDecimal("30");
    private static final Account ACCOUNT = new Account(
            "a1",
            "c1",
            new CheckingMovementPolicy(),
            BigDecimal.ZERO,
            null,
            null,
            new TransactionFee(0, BigDecimal.ONE));

    @Mock private MovementRepository movementRepository;

    @Mock private AccountClient accountClient;

    @Mock private YankiOperationRepository operationRepository;

    @Mock private YankiOperationResultPublisher resultPublisher;

    private ProcessYankiOperationUseCaseImpl useCase;

    @BeforeEach
    void createUseCase() {
        useCase = new ProcessYankiOperationUseCaseImpl(
                movementRepository,
                accountClient,
                operationRepository,
                resultPublisher);
    }

    private void newOperation(String operationId) {
        when(operationRepository.findById(operationId)).thenReturn(Maybe.empty());
        when(operationRepository.save(any())).thenReturn(Completable.complete());
        when(resultPublisher.publish(any())).thenReturn(Completable.complete());
    }

    private void accountWithBalance(String balance) {
        when(accountClient.findById("a1")).thenReturn(Maybe.just(ACCOUNT));
        when(movementRepository.countByAccountIdAndMonth("a1", YearMonth.from(DATE)))
                .thenReturn(Single.just(0L));
        when(movementRepository.getBalance("a1")).thenReturn(Single.just(new BigDecimal(balance)));
    }

    private Movement savedMovement() {
        ArgumentCaptor<Movement> movement = ArgumentCaptor.forClass(Movement.class);
        verify(movementRepository).save(movement.capture());
        return movement.getValue();
    }

    @Test
    void debitRegistersPaymentSentWithItsCommission() {
        newOperation("payment:p1:debit");
        accountWithBalance("100");
        when(movementRepository.save(any())).thenAnswer(inv -> Single.just(inv.getArgument(0)));

        useCase.execute("payment:p1:debit", true, "a1", AMOUNT, DATE, false).test()
                .assertComplete();

        Movement movement = savedMovement();
        assertThat(movement.type()).isEqualTo(MovementType.YANKI_PAYMENT_SENT);
        assertThat(movement.commission()).isEqualByComparingTo("1");
        verify(operationRepository).save(YankiOperation.completed("payment:p1:debit"));
        verify(resultPublisher).publish(YankiOperation.completed("payment:p1:debit"));
    }

    @Test
    void creditRegistersPaymentReceived() {
        newOperation("payment:p1:credit");
        accountWithBalance("0");
        when(movementRepository.save(any())).thenAnswer(inv -> Single.just(inv.getArgument(0)));

        useCase.execute("payment:p1:credit", false, "a1", AMOUNT, DATE, false).test()
                .assertComplete();

        assertThat(savedMovement().type()).isEqualTo(MovementType.YANKI_PAYMENT_RECEIVED);
    }

    @Test
    void reversalRegistersWithoutRulesOrCommission() {
        newOperation("payment:p1:refund");
        when(movementRepository.save(any())).thenAnswer(inv -> Single.just(inv.getArgument(0)));

        useCase.execute("payment:p1:refund", false, "a1", AMOUNT, DATE, true).test()
                .assertComplete();

        Movement movement = savedMovement();
        assertThat(movement.type()).isEqualTo(MovementType.YANKI_PAYMENT_REVERSAL);
        assertThat(movement.commission()).isEqualByComparingTo("0");
        verifyNoInteractions(accountClient);
    }

    @Test
    void brokenRuleOrMissingAccountIsRejectedWithItsReason() {
        newOperation("payment:p1:debit");
        accountWithBalance("10");

        useCase.execute("payment:p1:debit", true, "a1", AMOUNT, DATE, false).test()
                .assertComplete();

        YankiOperation rejected = YankiOperation.rejected(
                "payment:p1:debit",
                "Saldo insuficiente para el pago Yanki y su comisión");
        verify(operationRepository).save(rejected);
        verify(resultPublisher).publish(rejected);
        verify(movementRepository, never()).save(any());

        newOperation("payment:p2:debit");
        when(accountClient.findById("a9")).thenReturn(Maybe.empty());
        useCase.execute("payment:p2:debit", true, "a9", AMOUNT, DATE, false).test()
                .assertComplete();
        verify(resultPublisher)
                .publish(YankiOperation.rejected("payment:p2:debit", "La cuenta no existe"));
    }

    @Test
    void repeatedOperationRepublishesTheStoredResult() {
        YankiOperation stored = YankiOperation.completed("payment:p1:debit");
        when(operationRepository.findById("payment:p1:debit")).thenReturn(Maybe.just(stored));
        when(resultPublisher.publish(stored)).thenReturn(Completable.complete());

        useCase.execute("payment:p1:debit", true, "a1", AMOUNT, DATE, false).test()
                .assertComplete();

        verifyNoInteractions(accountClient, movementRepository);
        verify(operationRepository, never()).save(any());
    }

    @Test
    void unexpectedErrorsArePropagatedForRedelivery() {
        when(operationRepository.findById("payment:p1:debit")).thenReturn(Maybe.empty());
        when(accountClient.findById("a1"))
                .thenReturn(Maybe.error(new IllegalStateException("account-service down")));

        useCase.execute("payment:p1:debit", true, "a1", AMOUNT, DATE, false).test()
                .assertError(IllegalStateException.class);

        verifyNoInteractions(resultPublisher);
    }
}
