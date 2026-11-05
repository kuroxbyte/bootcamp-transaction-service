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
import dev.bootcamp.transactionservice.domain.model.account.AccountType;
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

    private void givenAccount(AccountType type, Integer maxMonthlyMovements, Integer movementDay) {
        when(accountClient.findById(1L))
                .thenReturn(Optional.of(new Account(1L, CUSTOMER_ID, type, maxMonthlyMovements, movementDay)));
    }

    @Test
    void customerDepositsIntoOwnAccount() {
        givenAccount(AccountType.CHECKING, null, null);
        when(movementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Movement movement = useCase.deposit(CUSTOMER_ID, 1L, AMOUNT, DATE);

        assertThat(movement).isEqualTo(new Movement(1L, MovementType.DEPOSIT, AMOUNT, DATE));
    }

    @Test
    void customerWithdrawsFromOwnAccount() {
        givenAccount(AccountType.CHECKING, null, null);
        when(movementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Movement movement = useCase.withdraw(CUSTOMER_ID, 1L, AMOUNT, DATE);

        assertThat(movement).isEqualTo(new Movement(1L, MovementType.WITHDRAWAL, AMOUNT, DATE));
    }

    @Test
    void rejectsMovementOnAccountOfAnotherCustomer() {
        givenAccount(AccountType.CHECKING, null, null);

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
        givenAccount(AccountType.SAVINGS, 2, null);
        when(movementRepository.countByAccountIdAndMonth(1L, YearMonth.from(DATE))).thenReturn(1L);

        useCase.deposit(CUSTOMER_ID, 1L, AMOUNT, DATE);

        verify(movementRepository).save(new Movement(1L, MovementType.DEPOSIT, AMOUNT, DATE));
    }

    @Test
    void doesNotSaveMovementOverSavingsLimit() {
        givenAccount(AccountType.SAVINGS, 2, null);
        when(movementRepository.countByAccountIdAndMonth(1L, YearMonth.from(DATE))).thenReturn(2L);

        assertThatThrownBy(() -> useCase.withdraw(CUSTOMER_ID, 1L, AMOUNT, DATE))
                .isInstanceOf(BusinessRuleException.class);
        verify(movementRepository, never()).save(any());
    }

    @Test
    void doesNotSaveSecondFixedTermMovementInMonth() {
        givenAccount(AccountType.FIXED_TERM, null, 15);
        when(movementRepository.countByAccountIdAndMonth(1L, YearMonth.from(DATE))).thenReturn(1L);

        assertThatThrownBy(() -> useCase.withdraw(CUSTOMER_ID, 1L, AMOUNT, DATE))
                .isInstanceOf(BusinessRuleException.class);
        verify(movementRepository, never()).save(any());
    }
}
