package dev.bootcamp.transactionservice.application.usecase;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.bootcamp.transactionservice.application.port.AccountClient;
import dev.bootcamp.transactionservice.application.port.MovementRepository;
import dev.bootcamp.transactionservice.domain.exception.BusinessRuleException;
import dev.bootcamp.transactionservice.domain.exception.NotFoundException;
import dev.bootcamp.transactionservice.domain.model.account.Account;
import dev.bootcamp.transactionservice.domain.model.account.CheckingMovementPolicy;
import dev.bootcamp.transactionservice.domain.model.account.FixedTermMovementPolicy;
import dev.bootcamp.transactionservice.domain.model.account.MovementPolicy;
import dev.bootcamp.transactionservice.domain.model.account.SavingsMovementPolicy;
import dev.bootcamp.transactionservice.domain.model.movement.Movement;
import dev.bootcamp.transactionservice.domain.model.movement.MovementType;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RegisterMovementUseCaseImplTest {

    private static final String CUSTOMER_ID = "100";
    private static final LocalDate DATE = LocalDate.of(2026, 11, 15);
    private static final BigDecimal AMOUNT = BigDecimal.TEN;

    @Mock
    private MovementRepository movementRepository;

    @Mock
    private AccountClient accountClient;

    @InjectMocks
    private RegisterMovementUseCaseImpl useCase;

    private void givenAccount(MovementPolicy movementPolicy) {
        when(accountClient.findById("1"))
                .thenReturn(Maybe.just(new Account("1", CUSTOMER_ID, movementPolicy)));
    }

    /** Cuenta propia con los movimientos del mes y el saldo indicados. */
    private void givenAccount(MovementPolicy movementPolicy, long movementsInMonth, String balance) {
        givenAccount(movementPolicy);
        when(movementRepository.countByAccountIdAndMonth("1", YearMonth.from(DATE)))
                .thenReturn(Single.just(movementsInMonth));
        when(movementRepository.getBalance("1")).thenReturn(Single.just(new BigDecimal(balance)));
    }

    private void givenSaveReturnsMovement() {
        when(movementRepository.save(any())).thenAnswer(inv -> Single.just(inv.getArgument(0)));
    }

    @Test
    void customerDepositsIntoOwnAccount() {
        givenAccount(new CheckingMovementPolicy(), 0, "0");
        givenSaveReturnsMovement();

        useCase.deposit(CUSTOMER_ID, "1", AMOUNT, DATE).test()
                .assertValue(new Movement("1", MovementType.DEPOSIT, AMOUNT, DATE));
    }

    @Test
    void customerWithdrawsFromOwnAccount() {
        givenAccount(new CheckingMovementPolicy(), 0, "100");
        givenSaveReturnsMovement();

        useCase.withdraw(CUSTOMER_ID, "1", AMOUNT, DATE).test()
                .assertValue(new Movement("1", MovementType.WITHDRAWAL, AMOUNT, DATE));
    }

    @Test
    void rejectsMovementOnAccountOfAnotherCustomer() {
        givenAccount(new CheckingMovementPolicy());

        useCase.deposit("999", "1", AMOUNT, DATE).test().assertError(NotFoundException.class);
        useCase.withdraw("999", "1", AMOUNT, DATE).test().assertError(NotFoundException.class);
        verify(movementRepository, never()).save(any());
    }

    @Test
    void rejectsInvalidAmountBeforeQueryingAccount() {
        useCase.deposit(CUSTOMER_ID, "1", BigDecimal.ZERO, DATE).test().assertError(BusinessRuleException.class);
        verify(accountClient, never()).findById(any());
        verify(movementRepository, never()).save(any());
    }

    @Test
    void rejectsUnknownAccount() {
        when(accountClient.findById("99")).thenReturn(Maybe.empty());

        useCase.deposit(CUSTOMER_ID, "99", AMOUNT, DATE).test().assertError(NotFoundException.class);
        verify(movementRepository, never()).save(any());
    }

    @Test
    void savesMovementWithinSavingsLimit() {
        givenAccount(new SavingsMovementPolicy(2), 1, "0");
        givenSaveReturnsMovement();

        useCase.deposit(CUSTOMER_ID, "1", AMOUNT, DATE).test().assertComplete();

        verify(movementRepository).save(new Movement("1", MovementType.DEPOSIT, AMOUNT, DATE));
    }

    @Test
    void doesNotSaveMovementOverSavingsLimit() {
        givenAccount(new SavingsMovementPolicy(2), 2, "100");

        useCase.withdraw(CUSTOMER_ID, "1", AMOUNT, DATE).test().assertError(BusinessRuleException.class);
        verify(movementRepository, never()).save(any());
    }

    @Test
    void doesNotSaveSecondFixedTermMovementInMonth() {
        givenAccount(new FixedTermMovementPolicy(15), 1, "100");

        useCase.withdraw(CUSTOMER_ID, "1", AMOUNT, DATE).test().assertError(BusinessRuleException.class);
        verify(movementRepository, never()).save(any());
    }

    @Test
    void doesNotSaveWithdrawalOverBalance() {
        givenAccount(new CheckingMovementPolicy(), 0, "5");

        useCase.withdraw(CUSTOMER_ID, "1", AMOUNT, DATE).test().assertError(BusinessRuleException.class);
        verify(movementRepository, never()).save(any());
    }

    @Test
    void depositIsSavedEvenWithZeroBalance() {
        givenAccount(new CheckingMovementPolicy(), 0, "0");
        givenSaveReturnsMovement();

        useCase.deposit(CUSTOMER_ID, "1", AMOUNT, DATE).test().assertComplete();

        verify(movementRepository).save(new Movement("1", MovementType.DEPOSIT, AMOUNT, DATE));
    }
}
