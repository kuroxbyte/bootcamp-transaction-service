package dev.bootcamp.transactionservice.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.bootcamp.transactionservice.application.port.AccountClient;
import dev.bootcamp.transactionservice.application.port.MovementRepository;
import dev.bootcamp.transactionservice.domain.exception.BusinessRuleException;
import dev.bootcamp.transactionservice.domain.model.account.Account;
import dev.bootcamp.transactionservice.domain.model.account.CheckingMovementPolicy;
import dev.bootcamp.transactionservice.domain.model.account.FixedTermMovementPolicy;
import dev.bootcamp.transactionservice.domain.model.account.MovementPolicy;
import dev.bootcamp.transactionservice.domain.model.account.SavingsMovementPolicy;
import dev.bootcamp.transactionservice.domain.model.movement.Movement;
import dev.bootcamp.transactionservice.domain.model.movement.MovementType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RegisterMovementUseCaseImplTest {

    private static final Long CUSTOMER_ID = 100L;
    private static final LocalDate DATE = LocalDate.of(2026, 11, 15);
    private static final BigDecimal AMOUNT = BigDecimal.TEN;

    @Mock
    private MovementRepository movementRepository;

    @Mock
    private AccountClient accountClient;

    @InjectMocks
    private RegisterMovementUseCaseImpl useCase;

    private void givenAccount(MovementPolicy movementPolicy) {
        when(accountClient.findById(1L))
                .thenReturn(Optional.of(new Account(1L, CUSTOMER_ID, movementPolicy)));
    }

    @Test
    void customerDepositsIntoOwnAccount() {
        givenAccount(new CheckingMovementPolicy());
        when(movementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Movement movement = useCase.deposit(CUSTOMER_ID, 1L, AMOUNT, DATE);

        assertThat(movement).isEqualTo(new Movement(1L, MovementType.DEPOSIT, AMOUNT, DATE));
    }

    @Test
    void customerWithdrawsFromOwnAccount() {
        givenAccount(new CheckingMovementPolicy());
        when(movementRepository.getBalance(1L)).thenReturn(new BigDecimal("100"));
        when(movementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Movement movement = useCase.withdraw(CUSTOMER_ID, 1L, AMOUNT, DATE);

        assertThat(movement).isEqualTo(new Movement(1L, MovementType.WITHDRAWAL, AMOUNT, DATE));
    }

    @Test
    void rejectsMovementOnAccountOfAnotherCustomer() {
        givenAccount(new CheckingMovementPolicy());

        assertThatThrownBy(() -> useCase.deposit(999L, 1L, AMOUNT, DATE))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> useCase.withdraw(999L, 1L, AMOUNT, DATE))
                .isInstanceOf(BusinessRuleException.class);
        verify(movementRepository, never()).save(any());
    }

    @Test
    void rejectsInvalidAmountBeforeQueryingAccount() {
        assertThatThrownBy(() -> useCase.deposit(CUSTOMER_ID, 1L, BigDecimal.ZERO, DATE))
                .isInstanceOf(BusinessRuleException.class);
        verify(accountClient, never()).findById(any());
        verify(movementRepository, never()).save(any());
    }

    @Test
    void rejectsUnknownAccount() {
        when(accountClient.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.deposit(CUSTOMER_ID, 99L, AMOUNT, DATE))
                .isInstanceOf(BusinessRuleException.class);
        verify(movementRepository, never()).save(any());
    }

    @Test
    void savesMovementWithinSavingsLimit() {
        givenAccount(new SavingsMovementPolicy(2));
        when(movementRepository.countByAccountIdAndMonth(1L, YearMonth.from(DATE))).thenReturn(1L);

        useCase.deposit(CUSTOMER_ID, 1L, AMOUNT, DATE);

        verify(movementRepository).save(new Movement(1L, MovementType.DEPOSIT, AMOUNT, DATE));
    }

    @Test
    void doesNotSaveMovementOverSavingsLimit() {
        givenAccount(new SavingsMovementPolicy(2));
        when(movementRepository.countByAccountIdAndMonth(1L, YearMonth.from(DATE))).thenReturn(2L);

        assertThatThrownBy(() -> useCase.withdraw(CUSTOMER_ID, 1L, AMOUNT, DATE))
                .isInstanceOf(BusinessRuleException.class);
        verify(movementRepository, never()).save(any());
    }

    @Test
    void doesNotSaveSecondFixedTermMovementInMonth() {
        givenAccount(new FixedTermMovementPolicy(15));
        when(movementRepository.countByAccountIdAndMonth(1L, YearMonth.from(DATE))).thenReturn(1L);

        assertThatThrownBy(() -> useCase.withdraw(CUSTOMER_ID, 1L, AMOUNT, DATE))
                .isInstanceOf(BusinessRuleException.class);
        verify(movementRepository, never()).save(any());
    }

    @Test
    void doesNotSaveWithdrawalOverBalance() {
        givenAccount(new CheckingMovementPolicy());
        when(movementRepository.getBalance(1L)).thenReturn(new BigDecimal("5"));

        assertThatThrownBy(() -> useCase.withdraw(CUSTOMER_ID, 1L, AMOUNT, DATE))
                .isInstanceOf(BusinessRuleException.class);
        verify(movementRepository, never()).save(any());
    }

    @Test
    void depositIsSavedEvenWithZeroBalance() {
        givenAccount(new CheckingMovementPolicy());
        when(movementRepository.getBalance(1L)).thenReturn(BigDecimal.ZERO);

        useCase.deposit(CUSTOMER_ID, 1L, AMOUNT, DATE);

        verify(movementRepository).save(new Movement(1L, MovementType.DEPOSIT, AMOUNT, DATE));
    }
}
