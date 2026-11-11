package dev.bootcamp.transactionservice.application.usecase;

import dev.bootcamp.transactionservice.application.port.AccountClient;
import dev.bootcamp.transactionservice.application.port.MovementRepository;
import dev.bootcamp.transactionservice.domain.exception.BusinessRuleException;
import dev.bootcamp.transactionservice.domain.exception.NotFoundException;
import dev.bootcamp.transactionservice.domain.model.account.Account;
import dev.bootcamp.transactionservice.domain.model.account.CheckingMovementPolicy;
import dev.bootcamp.transactionservice.domain.model.account.FixedTermMovementPolicy;
import dev.bootcamp.transactionservice.domain.model.account.MovementPolicy;
import dev.bootcamp.transactionservice.domain.model.account.TransactionFee;
import dev.bootcamp.transactionservice.domain.model.movement.Movement;
import dev.bootcamp.transactionservice.domain.model.movement.MovementType;
import dev.bootcamp.transactionservice.domain.model.transfer.Transfer;
import dev.bootcamp.transactionservice.domain.model.transfer.TransferType;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TransferUseCaseImplTest {

    private static final String CUSTOMER_ID = "100";
    private static final LocalDate DATE = LocalDate.of(2026, 11, 15);
    private static final BigDecimal AMOUNT = new BigDecimal("50");

    /**
     * 3 transacciones al mes sin comisión y 2 por cada transacción adicional.
     */
    private static final TransactionFee FEE = new TransactionFee(3, new BigDecimal("2"));

    @Mock private AccountClient accountClient;

    @Mock private MovementRepository movementRepository;

    private TransferUseCaseImpl useCase;

    @BeforeEach
    void createUseCase() {
        useCase = new TransferUseCaseImpl(accountClient, movementRepository);
        when(movementRepository.saveAll(any()))
                .thenAnswer(inv -> Flowable.fromIterable(inv.<List<Movement>>getArgument(0)));
    }

    /**
     * Cuenta con los movimientos del mes y el saldo de movimientos indicados.
     */
    private void givenAccount(
            String id,
            String customerId,
            MovementPolicy policy,
            long movementsInMonth,
            String balance) {
        when(accountClient.findById(id)).thenReturn(Maybe.just(
                new Account(id, customerId, policy, BigDecimal.ZERO, null, null, FEE)));
        when(movementRepository.countByAccountIdAndMonth(id, YearMonth.from(DATE)))
                .thenReturn(Single.just(movementsInMonth));
        when(movementRepository.getBalance(id)).thenReturn(Single.just(new BigDecimal(balance)));
    }

    @Test
    void transfersBetweenOwnAccountsSavingBothMovements() {
        givenAccount("a1", CUSTOMER_ID, new CheckingMovementPolicy(), 0, "100");
        givenAccount("a2", CUSTOMER_ID, new CheckingMovementPolicy(), 0, "0");

        Transfer transfer =
                useCase.transferToOwnAccount(CUSTOMER_ID, "a1", "a2", AMOUNT, DATE).blockingGet();

        assertThat(transfer.type()).isEqualTo(TransferType.OWN);
        verify(movementRepository).saveAll(List.of(
                new Movement("a1", MovementType.TRANSFER_OUT, AMOUNT, DATE),
                new Movement("a2", MovementType.TRANSFER_IN, AMOUNT, DATE)));
    }

    @Test
    void eachAccountPaysItsOwnCommission() {
        givenAccount("a1", CUSTOMER_ID, new CheckingMovementPolicy(), 3, "100");
        givenAccount("b1", "200", new CheckingMovementPolicy(), 0, "0");

        Transfer transfer =
                useCase.transferToThirdParty(CUSTOMER_ID, "a1", "b1", AMOUNT, DATE).blockingGet();

        assertThat(transfer.outgoing().commission()).isEqualByComparingTo("2");
        assertThat(transfer.incoming().commission()).isEqualByComparingTo("0");
    }

    @Test
    void sourceBalanceMustCoverAmountAndCommission() {
        givenAccount("a1", CUSTOMER_ID, new CheckingMovementPolicy(), 3, "51");
        givenAccount("b1", "200", new CheckingMovementPolicy(), 0, "0");

        useCase.transferToThirdParty(CUSTOMER_ID, "a1", "b1", AMOUNT, DATE).test()
                .assertError(BusinessRuleException.class);
        verify(movementRepository, never()).saveAll(any());
    }

    @Test
    void targetAccountRulesAlsoApply() {
        givenAccount("a1", CUSTOMER_ID, new CheckingMovementPolicy(), 0, "100");
        givenAccount("a2", CUSTOMER_ID, new FixedTermMovementPolicy(20), 0, "0");

        useCase.transferToOwnAccount(CUSTOMER_ID, "a1", "a2", AMOUNT, DATE).test()
                .assertError(BusinessRuleException.class);
        verify(movementRepository, never()).saveAll(any());
    }

    @Test
    void rejectsUnknownTargetAccount() {
        givenAccount("a1", CUSTOMER_ID, new CheckingMovementPolicy(), 0, "100");
        when(accountClient.findById("zz")).thenReturn(Maybe.empty());

        useCase.transferToThirdParty(CUSTOMER_ID, "a1", "zz", AMOUNT, DATE).test()
                .assertError(NotFoundException.class);
        verify(movementRepository, never()).saveAll(any());
    }

    @Test
    void rejectsSourceAccountOfAnotherCustomer() {
        givenAccount("b1", "200", new CheckingMovementPolicy(), 0, "100");
        givenAccount("a2", CUSTOMER_ID, new CheckingMovementPolicy(), 0, "0");

        useCase.transferToOwnAccount(CUSTOMER_ID, "b1", "a2", AMOUNT, DATE).test()
                .assertError(NotFoundException.class);
        verify(movementRepository, never()).saveAll(any());
    }
}
